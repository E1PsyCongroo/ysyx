package RV32E

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

class RV32E extends Module {
  val io = IO(new Bundle{

  })

}
