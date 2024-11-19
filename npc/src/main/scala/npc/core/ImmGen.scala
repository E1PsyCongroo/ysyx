package rvcpu.core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

import ImmType._

class ImmGenIO(xlen: Int) extends Bundle {
  val instruction = Input(UInt(32.W))
  val immType     = Input(UInt(ImmType.getWidth.W))
  val imm         = Output(UInt(xlen.W))
}

class ImmGen(xlen: Int) extends Module {
  val io     = IO(new ImmGenIO(xlen))
  val isImmI = io.immType === immI
  val isImmS = io.immType === immS
  val isImmB = io.immType === immB
  val isImmU = io.immType === immU
  val isImmJ = io.immType === immJ

  val inst     = io.instruction
  val imm0     = Mux(isImmI, inst(20), Mux(isImmS, inst(7), 0.U(1.W)))
  val imm4_1   = Mux(isImmI || isImmJ, inst(24, 21), Mux(isImmS || isImmB, inst(11, 8), 0.U(4.W)))
  val imm10_5  = Mux(isImmU, 0.U(6.W), inst(30, 25))
  val imm11    = Mux(isImmB, inst(7), Mux(isImmJ, inst(20), Mux(isImmU, 0.U(1.W), inst(31))))
  val imm19_12 = Mux(isImmU || isImmJ, inst(19, 12), Fill(8, inst(31)))
  val imm30_20 = Mux(isImmU, inst(30, 20), Fill(11, inst(31)))
  val imm31    = inst(31)
  io.imm := imm31 ## imm30_20 ## imm19_12 ## imm11 ## imm10_5 ## imm4_1 ## imm0
}
