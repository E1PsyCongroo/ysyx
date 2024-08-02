package RV32E

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import BrType._

class BrCondIO extends Bundle {
  val brType = Input(UInt(BrType.getWidth.W))
  val less = Input(Bool())
  val zero = Input(Bool())
  val PCASrc = Output(Bool())
  val PCBSrc = Output(Bool())
}

class BrCond extends Module {
  val io = IO(new BrCondIO)

  val isEq = io.zero
  val isNe = !io.zero
  val isLt = io.less
  val isGe = !io.less

  io.PCBSrc := Mux(io.brType === brJr.litValue.U, true.B, false.B)
  io.PCASrc := MuxLookup(io.brType, false.B)(Seq(
    brJ.litValue.U -> true.B,
    brEq.litValue.U -> isEq,
    brNe.litValue.U -> isNe,
    brLt.litValue.U -> isLt,
    brGe.litValue.U -> isGe,
  ))
}