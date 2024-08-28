package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class RVCPUIO (xlen: Int = 32) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val instruction = Input(UInt(32.W))
}

class RVCPU (
  xlen: Int = 32,
  extentionE: Boolean = true,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val io = IO(new RVCPUIO(xlen))

  val IFU = Module(new IFU(xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val WBU = Module(new WBU(xlen))

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  io.pc               := IFU.io.pc
  IFU.io.instruction  := io.instruction
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
