package RV32E

import chisel3._
import chisel3.util._

object InstructionType extends ChiselEnum {
  val RType, IType, SType, BType, UType, JType = Value
}

object ImmType extends ChiselEnum {
  val ImmI, ImmS, ImmB, ImmU, ImmJ = Value
}

object ALUType extends ChiselEnum {
  val aluAdd, aluSub, aluSll, aluSlt, aluSltu, aluBout, aluXor, aluSrl, aluSra, aluOr, aluAnd = Value
}