package rvcpu.core

import chisel3._
import chisel3.util._

import BrType._

class BrCondIO extends Bundle {
  val brType = Input(UInt(BrType.getWidth.W))
  val less   = Input(Bool())
  val zero   = Input(Bool())
  val PCASrc = Output(Bool())
  val PCBSrc = Output(Bool())
}

class BrCond extends Module {
  val io = IO(new BrCondIO)

  val isEq = io.zero
  val isNe = !io.zero
  val isLt = io.less
  val isGe = !io.less

  io.PCASrc := MuxCase(
    false.B,
    Seq(
      brJ -> true.B,
      brJr -> true.B,
      brEq -> isEq,
      brNe -> isNe,
      brLt -> isLt,
      brGe -> isGe
    ).map { case (key, data) => (io.brType === key, data) }
  )
  io.PCBSrc := Mux(io.brType === brJr, false.B, true.B)
}
