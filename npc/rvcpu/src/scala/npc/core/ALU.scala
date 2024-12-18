package rvcpu.core

import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

import ALUOutSel._

class ALUControl extends Bundle {
  val aluSel     = UInt(ALUOutSel.getWidth.W)
  val isArith    = Bool()
  val isLeft     = Bool()
  val isUnsigned = UInt(2.W)
  val isSub      = Bool()
}

class ALUIn(cpuConfig: CPUConfig) extends Bundle {
  val control = new ALUControl
  val inA     = UInt(cpuConfig.xlen.W)
  val inB     = UInt(cpuConfig.xlen.W)
}

class ALUOut(cpuConfig: CPUConfig) extends Bundle {
  val result = UInt(cpuConfig.xlen.W)
  val less   = Bool()
  val zero   = Bool()
}

class ALU(cpuConfig: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val flush = Input(Bool())
    val in    = Flipped(DecoupledIO(new ALUIn(cpuConfig)))
    val out   = DecoupledIO(new ALUOut(cpuConfig))
  })

  val in    = io.in.bits
  val adder = Adder(in.inA, in.inB, in.control.isSub)
  val mul   = Module(new WallaceMultiplier(cpuConfig.xlen, 4))
  val div   = Module(new BoothDivider(cpuConfig.xlen))

  mul.io.flush := io.flush
  mul.io.in.valid := io.in.valid && (in.control.aluSel === selectMul ||
    in.control.aluSel === selectMulh1 || in.control.aluSel === selectMulh2)
  mul.io.in.bits.signed       := ~in.control.isUnsigned
  mul.io.in.bits.multiplicand := in.inA
  mul.io.in.bits.multiplier   := in.inB
  mul.io.out.ready            := io.out.ready

  div.io.flush            := io.flush
  div.io.in.valid         := io.in.valid && (in.control.aluSel === selectDiv || in.control.aluSel === selectRem)
  div.io.in.bits.signed   := ~(in.control.isUnsigned.orR)
  div.io.in.bits.dividend := in.inA
  div.io.in.bits.divisor  := in.inB
  div.io.out.ready        := io.out.ready

  val less = Mux(
    in.control.isUnsigned.orR,
    adder.carry ^ in.control.isSub,
    adder.overflow ^ adder.result(cpuConfig.xlen - 1).asBool
  )
  val zero     = adder.zero
  val adderOut = adder.result.asUInt
  val shiftOut = BarrelShifter(in.inA, in.inB(4, 0), in.control.isLeft, in.control.isArith)
  val sltOut   = 0.U((cpuConfig.xlen - 1).W) ## less.asUInt
  val BOut     = in.inB
  val xorOut   = in.inA ^ in.inB
  val orOut    = in.inA | in.inB
  val andOut   = in.inA & in.inB
  val mulOut   = mul.io.out.bits.result_lo
  val mulhOut  = mul.io.out.bits.result_hi
  val divOut   = div.io.out.bits.quotient
  val remOut   = div.io.out.bits.remainder
  io.in.ready := mul.io.out.ready && div.io.out.ready
  io.out.bits.result := MuxCase(
    DontCare,
    Seq(
      selectAdder -> adderOut,
      selectShift -> shiftOut,
      selectSlt -> sltOut,
      selectB -> BOut,
      selectXor -> xorOut,
      selectOr -> orOut,
      selectAnd -> andOut,
      selectMul -> mulOut,
      selectMulh1 -> mulhOut,
      selectMulh2 -> mulhOut,
      selectDiv -> divOut,
      selectRem -> remOut
    ).map { case (key, data) => (in.control.aluSel === key, data) }
  )
  io.out.bits.less := less
  io.out.bits.zero := zero
  io.out.valid := MuxCase(
    io.in.valid,
    Seq(
      selectMul -> mul.io.out.valid,
      selectMulh1 -> mul.io.out.valid,
      selectMulh2 -> mul.io.out.valid,
      selectDiv -> div.io.out.valid,
      selectRem -> div.io.out.valid
    ).map { case (key, data) => (in.control.aluSel === key, data) }
  )
}
