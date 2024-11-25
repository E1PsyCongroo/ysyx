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

object StageConnect {
  var arch = "pipeline"

  def apply[T <: Data](
    leftOut:  DecoupledIO[T],
    rightIn:  DecoupledIO[T],
    rightOut: DecoupledIO[T],
    flush:    Option[Bool] = None
  ) = {
    if (arch == "single") {
      rightIn.bits  := leftOut.bits
      leftOut.ready := true.B
      rightIn.valid := true.B
    } else if (arch == "multi") {
      rightIn <> leftOut
    } else if (arch == "pipeline") {
      leftOut.ready := rightIn.ready
      rightIn.bits  := RegEnable(leftOut.bits, leftOut.fire)
      if (flush.isEmpty) {
        rightIn.valid := RegNext(Mux(leftOut.fire, true.B, Mux(rightOut.fire, false.B, rightIn.valid)))
      } else {
        rightIn.valid := RegNext(
          Mux(flush.get || rightOut.fire, false.B, Mux(leftOut.fire, true.B, rightIn.valid))
        )
      }
    } else if (arch == "ooo") {
      rightIn <> Queue(leftOut, 16)
    }
  }
}

class RVCPU(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("30000000", 16),
  sim:        Boolean = true)
    extends Module {
  val io = IO(new RVCPUIO(xlen))

  val needCache: UInt => Bool = addr =>
    Dev.MROMAddr.in(addr) || Dev.FlashAddr.in(addr) || Dev.ChipLinkMEMAddr.in(addr) || Dev.PSRAMAddr.in(
      addr
    ) || Dev.SDRAMAddr.in(addr)
  val IFU = Module(new IFU(awidth, xlen, PCReset, 6, 5, 0, needCache, sim))
  val IDU = Module(new IDU(xlen, extentionE, sim))
  val EXU = Module(new EXU(xlen, extentionE, sim))
  val LSU = Module(new LSU(xlen, extentionE, sim))
  val WBU = Module(new WBU(xlen, extentionE, sim))

  StageConnect(IFU.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
  StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
  StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
  StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

  IFU.io.jump       := EXU.io.jump
  IFU.io.nextPC     := EXU.io.nextPC
  IFU.io.cacheFlush := IDU.io.fence_i

  // TODO: 解决IDU指令不需要ra1 ra2访问时误判RAW的情况
  def conflict(ra1: UInt, ra2: UInt, wa: UInt, we: Bool) = we && wa =/= 0.U && ra1 === wa && ra2 === wa
  val isRAW = conflict(
    IDU.io.RegFileAccess.ra1,
    IDU.io.RegFileAccess.ra2,
    EXU.io.RegFileAccess.wa,
    EXU.io.RegFileAccess.we
  ) || conflict(
    IDU.io.RegFileAccess.ra1,
    IDU.io.RegFileAccess.ra2,
    LSU.io.RegFileAccess.wa,
    LSU.io.RegFileAccess.we
  ) || conflict(IDU.io.RegFileAccess.ra1, IDU.io.RegFileAccess.ra2, WBU.io.RegFileAccess.wa, WBU.io.RegFileAccess.we)
  IDU.io.stall := isRAW

  WBU.io.out.ready := true.B

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  val CLINT = Module(new CLINT(awidth, xlen, Dev.CLINTAddr))

  AXI4Interconnect(
    Seq(LSU.io.master, IFU.io.master),
    Seq(Dev.CLINTAddr.in, !Dev.CLINTAddr.in(_)),
    Seq(CLINT.io, io.master)
  )
  io.slave <> AXI4.none

  if (sim) {
    EndControl(clock, WBU.io.out.fire && WBU.io.out.bits.isEnd.get, WBU.io.out.bits.exitCode.get)

    val devs = Seq(
      Dev.CLINTAddr,
      Dev.UART16550Addr,
      Dev.SPIMasterAddr,
      Dev.GPIOAddr,
      Dev.PS2Addr,
      Dev.VGAAddr,
      Dev.ChipLinkMEMAddr
    )
    val needSkipDifftest = devs.map { dev => dev.in(LSU.io.in.bits.aluOut) }.foldLeft(false.B)(_ || _)
    SkipDifftest(clock, LSU.io.out.fire && needSkipDifftest);

    val IFUArfire = IFU.io.master.arvalid && IFU.io.master.arready
    val IFURfire  = IFU.io.master.rvalid && IFU.io.master.rready
    val IFUTracer = Module(new Tracer)
    IFUTracer.io.raddr := RegEnable(IFU.io.master.araddr, IFUArfire)
    IFUTracer.io.rlen  := RegEnable(1.U << IFU.io.master.arsize, IFUArfire)
    IFUTracer.io.rdata := IFU.io.master.rdata
    IFUTracer.io.ren   := IFURfire
    IFUTracer.io.waddr := DontCare
    IFUTracer.io.wdata := DontCare
    IFUTracer.io.wlen  := DontCare
    IFUTracer.io.wen   := false.B

    val LSUArfire = LSU.io.master.arvalid && LSU.io.master.arready
    val LSURfire  = LSU.io.master.rvalid && LSU.io.master.rready
    val LSUAwfire = LSU.io.master.awvalid && LSU.io.master.awready
    val LSUWfire  = LSU.io.master.wvalid && LSU.io.master.wready
    val LSUBfire  = LSU.io.master.bvalid && LSU.io.master.bready
    val LSUTracer = Module(new Tracer)
    LSUTracer.io.raddr := RegEnable(LSU.io.master.araddr, LSUArfire)
    LSUTracer.io.rlen  := RegEnable(1.U << LSU.io.master.arsize, LSUArfire)
    LSUTracer.io.rdata := LSU.io.master.rdata
    LSUTracer.io.ren   := LSURfire
    LSUTracer.io.waddr := RegEnable(LSU.io.master.awaddr, LSUAwfire)
    LSUTracer.io.wdata := RegEnable(LSU.io.master.wdata, LSUWfire)
    LSUTracer.io.wlen  := RegEnable(1.U << LSU.io.master.awsize, LSUAwfire)
    LSUTracer.io.wen   := LSUBfire

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.master.arvalid
    TracerDataFetch.io.finish := LSU.io.master.rvalid && LSU.io.master.rready

    val CacheTracer = Module(new CacheTracer)
    CacheTracer.io.cacheNeed         := IFU.io.ICacheTrace.get.need
    CacheTracer.io.cacheHit          := IFU.io.ICacheTrace.get.hit
    CacheTracer.io.cacheAccessStart  := IFU.io.ICacheTrace.get.accessStart
    CacheTracer.io.cacheAccessFinish := IFU.io.ICacheTrace.get.accessFin
    CacheTracer.io.cacheFetchStart   := IFU.io.ICacheTrace.get.missStart
    CacheTracer.io.cacheFetchFinish  := IFU.io.ICacheTrace.get.missFin
  }
}

class NPC(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("80000000", 16),
  sim:        Boolean = true)
    extends Module {

  val io = IO(new Bundle {
    val nextPC = if (sim) Some(Output(UInt(xlen.W))) else None
    val inst   = if (sim) Some(Output(UInt(32.W))) else None
  })

  val needCache: UInt => Bool = Dev.memoryAddr.in
  val IFU = Module(new IFU(awidth, xlen, PCReset, 6, 5, 0, needCache, sim))
  val IDU = Module(new IDU(xlen, extentionE, sim))
  val EXU = Module(new EXU(xlen, extentionE, sim))
  val LSU = Module(new LSU(xlen, extentionE, sim))
  val WBU = Module(new WBU(xlen, extentionE, sim))

  StageConnect(IFU.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
  StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
  StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
  StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

  IFU.io.jump       := EXU.io.jump
  IFU.io.nextPC     := EXU.io.nextPC
  IFU.io.cacheFlush := IDU.io.fence_i

  // TODO: 解决IDU指令不需要ra1 ra2访问时误判RAW的情况
  def conflict(ra1: UInt, ra2: UInt, wa: UInt, we: Bool) = we && wa =/= 0.U && (ra1 === wa || ra2 === wa)
  val isRAW = conflict(
    IDU.io.RegFileAccess.ra1,
    IDU.io.RegFileAccess.ra2,
    EXU.io.RegFileAccess.wa,
    EXU.io.RegFileAccess.we
  ) || conflict(
    IDU.io.RegFileAccess.ra1,
    IDU.io.RegFileAccess.ra2,
    LSU.io.RegFileAccess.wa,
    LSU.io.RegFileAccess.we
  ) || conflict(IDU.io.RegFileAccess.ra1, IDU.io.RegFileAccess.ra2, WBU.io.RegFileAccess.wa, WBU.io.RegFileAccess.we)
  IDU.io.stall := isRAW

  WBU.io.out.ready := true.B

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  val AXI4Mem = Module(new AXI4Mem(awidth, xlen))
  val CLINT   = Module(new CLINT(awidth, xlen, Dev.mtimeAddr))
  val Uart    = Module(new Uart(awidth, xlen))

  AXI4Interconnect(
    Seq(LSU.io.master, IFU.io.master),
    Seq(Dev.mtimeAddr.in, Dev.uartAddr.in, addr => !Dev.mtimeAddr.in(addr) && !Dev.uartAddr.in(addr)),
    Seq(CLINT.io, Uart.io, AXI4Mem.io)
  )

  if (sim) {
    EndControl(clock, WBU.io.out.fire && WBU.io.out.bits.isEnd.get, WBU.io.out.bits.exitCode.get)

    val devs = Seq(
      Dev.mtimeAddr,
      Dev.uartAddr
    )
    val needSkipDifftest = devs.map { dev => dev.in(LSU.io.in.bits.aluOut) }.foldLeft(false.B)(_ || _)
    SkipDifftest(clock, LSU.io.out.fire && needSkipDifftest)

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.master.arvalid
    TracerDataFetch.io.finish := LSU.io.master.rvalid && LSU.io.master.rready

    val CacheTracer = Module(new CacheTracer)
    CacheTracer.io.cacheNeed         := IFU.io.ICacheTrace.get.need
    CacheTracer.io.cacheHit          := IFU.io.ICacheTrace.get.hit
    CacheTracer.io.cacheAccessStart  := IFU.io.ICacheTrace.get.accessStart
    CacheTracer.io.cacheAccessFinish := IFU.io.ICacheTrace.get.accessFin
    CacheTracer.io.cacheFetchStart   := IFU.io.ICacheTrace.get.missStart
    CacheTracer.io.cacheFetchFinish  := IFU.io.ICacheTrace.get.missFin

    io.nextPC.get := WBU.io.out.bits.nextPC.get
    io.inst.get   := WBU.io.out.bits.inst.get
  }
}
