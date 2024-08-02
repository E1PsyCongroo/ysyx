package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class EndControlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val isEnd = Input(Bool())
}
class EndControl extends BlackBox with HasBlackBoxResource {
  val io = IO(new EndControlIO)
  addResource("/EndControl.sv")
}

class RVCPU(xlen: Int, extentionE: Boolean) extends Module {
  val io = IO(new Bundle{
    val pc = Output(UInt())
  })

}
