package RV32E

import chisel3._
import chisel3.util._

object InstructionType extends ChiselEnum {
  val RType, IType, SType, BType, UType, JType = Value
}

object ImmType extends ChiselEnum {
  val ImmI, ImmS, ImmB, ImmU, ImmJ = Value
}

object ALUOp {
  def getWidth = 4
  val aluAdd  = BitPat("b0000")
  val aluSub  = BitPat("b1000")
  val aluSll  = BitPat("b?001")
  val aluSlt  = BitPat("b0010")
  val aluSltu = BitPat("b1010")
  val aluBout = BitPat("b?011")
  val aluXor  = BitPat("b?100")
  val aluSrl  = BitPat("b0101")
  val aluSra  = BitPat("b1101")
  val aluOr   = BitPat("b?110")
  val aluAnd  = BitPat("b?111")
}

object BrType extends ChiselEnum {
  val brNone, brJr, brJ, brEq, brNe, brLt, brGe = Value
}

object MemOp {
  def getWidth = 3
  val memB = BitPat("b000")
  val memH = BitPat("b001")
  val memW = BitPat("b010")
  val memBu = BitPat("b100")
  val memHu = BitPat("b101")
}

object ALUASrcFrom extends ChiselEnum {
  val fromRs1, fromPc = Value
}

object ALUBSrcFrom extends ChiselEnum {
  val fromRs2, fromImm, from4 = Value
}

object WBSrcFrom extends ChiselEnum {
  val fromALU, fromMem = Value
}