package rvcpu.core

import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

import ALUOutSel._

class ALUOut(width: Int) extends Bundle {
  val out  = Output(UInt(width.W))
  val less = Output(Bool())
  val zero = Output(Bool())
}

object ALU {
  def apply(aluSel: UInt, isArith: Bool, isLeft: Bool, isUnsigned: Bool, isSub: Bool, inA: UInt, inB: UInt) = {
    require(aluSel.getWidth == ALUOutSel.getWidth)
    require(inA.getWidth == inB.getWidth)
    val width = inA.getWidth
    val out   = Wire(new ALUOut(width))

    val adder = Adder(inA, inB, isSub)
    val less = Mux(
      isUnsigned,
      adder.carry ^ isSub,
      adder.overflow ^ adder.result(width - 1).asBool
    )
    val zero     = adder.zero
    val adderOut = adder.result.asUInt
    val shiftOut = BarrelShifter(inA, inB(4, 0), isLeft, isArith)
    // val shiftOut = Shifter(inA, inB(4, 0), isLeft, isArith)
    val sltOut   = 0.U((width - 1).W) ## less.asUInt
    val BOut     = inB
    val xorOut   = inA ^ inB
    val orOut    = inA | inB
    val andOut   = inA & inB
    out.out := MuxCase(
      DontCare,
      Seq(
        selectAdder -> adderOut,
        selectShift -> shiftOut,
        selectSlt -> sltOut,
        selectB -> BOut,
        selectXor -> xorOut,
        selectOr -> orOut,
        selectAnd -> andOut
      ).map { case (key, data) => (aluSel === key, data) }
    )
    out.less := less
    out.zero := zero
    out
  }
}
