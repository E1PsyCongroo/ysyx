package rvcpu.core

import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

import ALUOutSel._

class ALUIO(xlen: Int = 32) extends Bundle {
  val aluSel     = Input(UInt(ALUOutSel.getWidth.W))
  val isArith    = Input(Bool())
  val isLeft     = Input(Bool())
  val isUnsigned = Input(Bool())
  val isSub      = Input(Bool())
  val inA        = Input(UInt(xlen.W))
  val inB        = Input(UInt(xlen.W))
  val aluOut     = Output(UInt(xlen.W))
  val less       = Output(Bool())
  val zero       = Output(Bool())
}

class ALU(xlen: Int = 32) extends Module {
  val io = IO(new ALUIO(xlen))

  val adder       = Module(new Adder(xlen))
  val barrelShift = Module(new BarrelShift(xlen))

  adder.io.addSub := io.isSub
  adder.io.ina    := io.inA.asSInt
  adder.io.inb    := io.inB.asSInt

  barrelShift.io.in      := io.inA
  barrelShift.io.shamt   := io.inB(4, 0)
  barrelShift.io.isArith := io.isArith
  barrelShift.io.isLeft  := io.isLeft

  val less = Mux(
    io.isUnsigned,
    adder.io.carry ^ io.isSub,
    adder.io.overflow ^ adder.io.result(xlen - 1).asBool
  )
  val zero     = adder.io.zero
  val adderOut = adder.io.result.asUInt
  val shiftOut = barrelShift.io.out
  val sltOut   = 0.U((xlen - 1).W) ## less.asUInt
  val BOut     = io.inB
  val xorOut   = io.inA ^ io.inB
  val orOut    = io.inA | io.inB
  val andOut   = io.inA & io.inB
  val aluOut = MuxCase(
    DontCare,
    Seq(
      selectAdder -> adderOut,
      selectShift -> shiftOut,
      selectSlt -> sltOut,
      selectB -> BOut,
      selectXor -> xorOut,
      selectOr -> orOut,
      selectAnd -> andOut
    ).map { case (key, data) => (io.aluSel === key, data) }
  )

  io.aluOut := aluOut
  io.less   := less
  io.zero   := zero
}
