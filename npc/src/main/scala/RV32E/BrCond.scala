package RVCPU

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

  io.PCASrc := MuxLookup(io.brType, false.B)(Seq(
    brJ.asUInt -> true.B,
    brJr.asUInt -> true.B,
    brEq.asUInt -> isEq,
    brNe.asUInt -> isNe,
    brLt.asUInt -> isLt,
    brGe.asUInt -> isGe,
  ))
  io.PCBSrc := Mux(io.brType === brJr.asUInt, false.B, true.B)
}