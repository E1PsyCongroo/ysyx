package rvcpu.core

import chisel3._
import chisel3.util._

import BrType._

object BrCond {
  def apply(brType: UInt, less: Bool, zero: Bool): Bool = {
    require(brType.getWidth == BrType.getWidth)
    val isEq = zero
    val isNe = !zero
    val isLt = less
    val isGe = !less
    val out  = Wire(Bool())
    out := MuxLookup(brType, false.B)(
      Seq(
        brNone -> false.B,
        brJ -> true.B,
        brEq -> isEq,
        brNe -> isNe,
        brLt -> isLt,
        brGe -> isGe
      ).map { case (key, data) => (key.value.U, data) }
    )
    out
  }
}
