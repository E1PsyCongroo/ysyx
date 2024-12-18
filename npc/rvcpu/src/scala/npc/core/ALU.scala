package rvcpu.core

import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

import ALUOutSel._

class ALUIn(cpuConfig: CPUConfig) extends Bundle {
  val aluSel = Input(UInt(ALUOutSel.getWidth.W))
  val isArith = Input(Bool())
  val isLeft = Input(Bool())
  val isUnsigned = Input(Bool())
  val isSub = Input(Bool())
  val inA = Input(UInt(cpuConfig.xlen.W))
  val inB = Input(UInt(cpuConfig.xlen.W))
}

class ALUOut(cpuConfig: CPUConfig) extends Bundle {
  val result  = Output(UInt(cpuConfig.xlen.W))
  val less = Output(Bool())
  val zero = Output(Bool())
}

class ALU(cpuConfig: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val in = new ALUIn(cpuConfig)
    val out = new ALUOut(cpuConfig)
  })

  val adder = Adder(io.in.inA, io.in.inB, io.in.isSub)
  val less = Mux(
    io.in.isUnsigned,
    adder.carry ^ io.in.isSub,
    adder.overflow ^ adder.result(cpuConfig.xlen - 1).asBool
  )
  val zero     = adder.zero
  val adderOut = adder.result.asUInt
  val shiftOut = BarrelShifter(io.in.inA, io.in.inB(4, 0), io.in.isLeft, io.in.isArith)
  // val shiftOut = Shifter(io.in.inA, io.in.inB(4, 0), io.in.isLeft, io.in.isArith)
  val sltOut = 0.U((cpuConfig.xlen - 1).W) ## less.asUInt
  val BOut   = io.in.inB
  val xorOut = io.in.inA ^ io.in.inB
  val orOut  = io.in.inA | io.in.inB
  val andOut = io.in.inA & io.in.inB
  io.out.result := MuxCase(
    DontCare,
    Seq(
      selectAdder -> adderOut,
      selectShift -> shiftOut,
      selectSlt -> sltOut,
      selectB -> BOut,
      selectXor -> xorOut,
      selectOr -> orOut,
      selectAnd -> andOut
    ).map { case (key, data) => (io.in.aluSel === key, data) }
  )
  io.out.less := less
  io.out.zero := zero
}
