package rvcpu.core

import chisel3._
import chisel3.util._

import BrType._

class BrCond(cpuConfig: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val brType = Input(UInt(BrType.getWidth.W))
    val less   = Input(Bool())
    val zero   = Input(Bool())
    val jump   = Output(Bool())
  })
  val isEq = io.zero
  val isNe = !io.zero
  val isLt = io.less
  val isGe = !io.less
  io.jump := MuxLookup(io.brType, false.B)(
    Seq(
      brNone -> false.B,
      brJ -> true.B,
      brEq -> isEq,
      brNe -> isNe,
      brLt -> isLt,
      brGe -> isGe
    ).map { case (key, data) => (key.value.U, data) }
  )
}
