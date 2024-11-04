package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class RVCPUIO(awidth: Int = 32, xlen: Int = 32) extends Bundle {
  val interrupt = Input(Bool())
  val master    = new AXI4MasterIO
  val slave     = Flipped(new AXI4MasterIO)
}

class RVCPU(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("80000000", 16),
  sim:        Boolean = true)
    extends Module {
  val io = IO(new RVCPUIO(xlen))

  val IFU = Module(new IFU(xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.LSUIn <> LSU.io.in
  LSU.io.out <> EXU.io.LSUOut

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val CLINT   = Module(new CLINT(awidth, xlen))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  val AXI4Interconnect = Module(new AXI4Interconnect(2, Seq(Dev.CLINTAddr.in, !Dev.CLINTAddr.in(_))))
  AXI4Interconnect.io.fanIn(0) <> IFU.io.master
  AXI4Interconnect.io.fanIn(1) <> LSU.io.master
  AXI4Interconnect.io.fanOut(0) <> CLINT.io
  AXI4Interconnect.io.fanOut(1) <> io.master
  io.slave <> AXI4.none

  if (sim) {
    EndControl(clock, IDU.io.isEnd, IDU.io.exitCode)

    val devs = Seq(
      Dev.CLINTAddr,
      Dev.UART16550Addr,
      Dev.SPIMasterAddr,
      Dev.GPIOAddr,
      Dev.PS2Addr,
      Dev.VGAAddr,
      Dev.ChipLinkMEMAddr
    )
    SkipDifftest(
      clock,
      LSU.io.in.fire &&
        (devs.map(dev => dev.in(LSU.io.in.bits.waddr) || dev.in(LSU.io.in.bits.raddr)).foldLeft(false.B)(_ || _))
    );

    val IFUTracer = Module(new Tracer)
    IFUTracer.io.raddr := IFU.io.out.bits.pc
    IFUTracer.io.rlen  := 4.U
    IFUTracer.io.rdata := IFU.io.out.bits.instruction
    IFUTracer.io.ren   := IFU.io.out.fire
    IFUTracer.io.waddr := DontCare
    IFUTracer.io.wdata := DontCare
    IFUTracer.io.wlen  := DontCare
    IFUTracer.io.wen   := false.B

    val LSUTracer = Module(new Tracer)
    val LSUIn     = RegEnable(LSU.io.in.bits, LSU.io.in.fire)
    val LSUSize = MuxCase(
      4.U(32.W),
      Seq(
        MemOp.memB -> 1.U(32.W),
        MemOp.memH -> 2.U(32.W),
        MemOp.memW -> 4.U(32.W),
        MemOp.memBu -> 1.U(32.W),
        MemOp.memHu -> 2.U(32.W)
      ).map { case (key, data) => (key === LSUIn.memOp, data) }
    )
    LSUTracer.io.raddr := LSUIn.raddr
    LSUTracer.io.rlen  := LSUSize
    LSUTracer.io.rdata := LSU.io.out.bits.rdata
    LSUTracer.io.ren   := LSUIn.ren && LSU.io.out.fire
    LSUTracer.io.waddr := LSUIn.waddr
    LSUTracer.io.wdata := LSUIn.wdata
    LSUTracer.io.wlen  := LSUSize
    LSUTracer.io.wen   := LSUIn.wen && LSU.io.out.fire

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.in.fire && LSU.io.in.bits.ren
    TracerDataFetch.io.finish := LSU.io.out.fire
  }
}

object StageConnect {
  var arch = "multi"

  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    if (arch == "single") {
      right.bits  := left.bits
      left.ready  := true.B
      right.valid := true.B
    } else if (arch == "multi") {
      right <> left
    } else if (arch == "pipeline") {
      right <> RegEnable(left, left.fire)
    } else if (arch == "ooo") {
      right <> Queue(left, 16)
    }
  }
}

class NPC(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("80000000", 16),
  sim:        Boolean = true)
    extends Module {
  val IFU = Module(new IFU(xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.LSUIn <> LSU.io.in
  LSU.io.out <> EXU.io.LSUOut

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val CLINT   = Module(new CLINT(awidth, xlen))
  val AXI4Mem = Module(new AXI4Mem(awidth, xlen))
  val Uart    = Module(new Uart(awidth, xlen))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  val AXI4Interconnect = Module(new AXI4Interconnect(2, Seq(Dev.CLINTAddr.in,  Dev.uartAddr.in, !Dev.CLINTAddr.in(_))))
  AXI4Interconnect.io.fanIn(0) <> IFU.io.master
  AXI4Interconnect.io.fanIn(1) <> LSU.io.master
  AXI4Interconnect.io.fanOut(0) <> CLINT.io
  AXI4Interconnect.io.fanOut(1) <> Uart.io
  AXI4Interconnect.io.fanOut(2) <> AXI4Mem.io

  if (sim) {
    EndControl(clock, IDU.io.isEnd, IDU.io.exitCode)

    val devs = Seq(
      Dev.uartAddr,
    )
    SkipDifftest(
      clock,
      LSU.io.in.fire &&
        (devs.map(dev => dev.in(LSU.io.in.bits.waddr) || dev.in(LSU.io.in.bits.raddr)).foldLeft(false.B)(_ || _))
    );

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.in.fire && LSU.io.in.bits.ren
    TracerDataFetch.io.finish := LSU.io.out.fire
  }
}
