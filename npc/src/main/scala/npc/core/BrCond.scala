package rvcpu.core

import chisel3._
import chisel3.util._

import BrType._

class BrCondIO extends Bundle {
  val brType = Input(UInt(BrType.getWidth.W))
  val less   = Input(Bool())
  val zero   = Input(Bool())
  val jump   = Output(Bool())
}

class BrCond extends Module {
  val io = IO(new BrCondIO)

  val isEq = io.zero
  val isNe = !io.zero
  val isLt = io.less
  val isGe = !io.less

  io.jump := MuxCase(
    DontCare,
    Seq(
      brNone -> false.B,
      brJ -> true.B,
      brEq -> isEq,
      brNe -> isNe,
      brLt -> isLt,
      brGe -> isGe
    ).map { case (key, data) => (io.brType === key, data) }
  )
}
