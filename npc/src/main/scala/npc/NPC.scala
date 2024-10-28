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
  val slave     = new AXI4SlaveIO
}

class RVCPU(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("80000000", 16))
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

  EndControl(clock, IDU.io.isEnd, IDU.io.exitCode)

  val AXI4Interconnect = Module(new AXI4Interconnect(2, Seq(Dev.CLINTAddr.in, !Dev.CLINTAddr.in(_))))
  AXI4Interconnect.io.fanIn(0) <> IFU.io.master
  AXI4Interconnect.io.fanIn(1) <> LSU.io.master
  AXI4Interconnect.io.fanOut(0).AXI4ConnectSlave(CLINT.io)
  AXI4Interconnect.io.fanOut(1) <> io.master
  io.slave <> AXI4Slave.nonResp

  val devs = Seq(Dev.CLINTAddr, Dev.UART16550Addr, Dev.SPIMasterAddr, Dev.GPIOAddr, Dev.PS2Addr, Dev.VGAAddr, Dev.ChipLinkMEMAddr)
  SkipDifftest(
    clock,
    LSU.io.in.fire &&
      (devs.map(dev => dev.in(LSU.io.in.bits.waddr) || dev.in(LSU.io.in.bits.raddr)).foldLeft(false.B)(_ || _))
  );

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
