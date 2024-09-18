package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class RVCPUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val interrupt = Input(Bool())
  val master    = new AXI4MasterIO
  val slave     = new AXI4SlaveIO
}

class RVCPU (
  awidth: Int = 32,
  xlen: Int = 32,
  extentionE: Boolean = true,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val io = IO(new RVCPUIO(xlen))

  val IFU = Module(new IFU(xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.LSUIn <> LSU.io.in
  LSU.io.out <> EXU.io.LSUOut

  val RegFile    = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1 := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2 := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa  := WBU.io.RegFileAccess.wa
  RegFile.io.we  := WBU.io.RegFileAccess.we
  RegFile.io.wd  := WBU.io.RegFileAccess.wd

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  io.master <> AXI4Arbiter(Seq(IFU.io.master, LSU.io.master))
  io.slave <> AXI4Slave.nonResp
}

object StageConnect {
  var arch = "multi"

  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    if (arch == "single")   {
      right.bits  := left.bits
      left.ready  := true.B
      right.valid := true.B
    }
    else if (arch == "multi") {
      right <> left
    }
    else if (arch == "pipeline") {
      right <> RegEnable(left, left.fire)
    }
    else if (arch == "ooo") {
      right <> Queue(left, 16)
    }
  }
}

class NPC (
  awidth: Int = 32,
  xlen: Int = 32,
  extentionE: Boolean = true,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val RVCPU           = Module(new RVCPU(awidth, xlen, extentionE, PCReset))
  val AXILiteMem      = Module(new AXILiteMem(awidth, xlen, 4))
  val Uart            = Module(new Uart(awidth, xlen, 4))
  val CLINT           = Module(new CLINT(awidth, xlen, 4))
  AXI4Xbar(
    RVCPU.io.master,
    Seq(
      (rvcpu.dev.Dev.memoryAddr, AXILiteMem.io),
      (rvcpu.dev.Dev.uartAddr, Uart.io),
      (rvcpu.dev.Dev.mtimeAddr, CLINT.io),
    ),
  )
}