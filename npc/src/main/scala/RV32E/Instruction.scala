package RVCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

case class Instruction(
  val funct7: BitPat = BitPat.dontCare(7),
  val rs2: BitPat = BitPat.dontCare(5),
  val rs1: BitPat = BitPat.dontCare(5),
  val funct3: BitPat = BitPat.dontCare(3),
  val rd: BitPat = BitPat.dontCare(5),
  val opcode: BitPat
) extends DecodePattern {
  require(
    funct7.getWidth == 7 && rs2.getWidth == 5 && rs1.getWidth == 5 &&
    funct3.getWidth == 3 && rd.getWidth == 5 && opcode.getWidth == 7
  )
  def bitPat: BitPat = funct7 ## rs2 ## rs1 ## funct3 ## rd ## opcode
}

object Instruction {
  object InstructionType extends ChiselEnum {
    val RType, IType, SType, BType, UType, JType = Value
  }

  object InstricitonMap {
    val LUI     = Instruction(opcode = BitPat("b0110111"))
    val AUIPC   = Instruction(opcode = BitPat("b0010111"))
    val JAL     = Instruction(opcode = BitPat("b1101111"))
    val JALR    = Instruction(opcode = BitPat("b1100111"), funct3 = BitPat("b000"))
    val BEQ     = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b000"))
    val BNE     = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b001"))
    val BLT     = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b100"))
    val BGE     = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b101"))
    val BLTU    = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b110"))
    val BGEU    = Instruction(opcode = BitPat("b1100011"), funct3 = BitPat("b111"))
    val LB      = Instruction(opcode = BitPat("b0000011"), funct3 = BitPat("b000"))
    val LH      = Instruction(opcode = BitPat("b0000011"), funct3 = BitPat("b001"))
    val LW      = Instruction(opcode = BitPat("b0000011"), funct3 = BitPat("b010"))
    val LBU     = Instruction(opcode = BitPat("b0000011"), funct3 = BitPat("b100"))
    val LHU     = Instruction(opcode = BitPat("b0000011"), funct3 = BitPat("b101"))
    val SB      = Instruction(opcode = BitPat("b0100011"), funct3 = BitPat("b000"))
    val SH      = Instruction(opcode = BitPat("b0100011"), funct3 = BitPat("b001"))
    val SW      = Instruction(opcode = BitPat("b0100011"), funct3 = BitPat("b010"))
    val ADDI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b000"))
    val SLTI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b010"))
    val SLTIU   = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b011"))
    val XORI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b100"))
    val ORI     = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b110"))
    val ANDI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b111"))
    val SLLI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b001"), funct7 = BitPat("b0000000"))
    val SRLI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b101"), funct7 = BitPat("b0000000"))
    val SRAI    = Instruction(opcode = BitPat("b0010011"), funct3 = BitPat("b101"), funct7 = BitPat("b0100000"))
    val ADD     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"))
    val SUB     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0100000"))
    val SLL     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b001"), funct7 = BitPat("b0000000"))
    val SLT     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b010"), funct7 = BitPat("b0000000"))
    val SLTU    = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b011"), funct7 = BitPat("b0000000"))
    val XOR     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b100"), funct7 = BitPat("b0000000"))
    val SRL     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b101"), funct7 = BitPat("b0000000"))
    val SRA     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b101"), funct7 = BitPat("b0100000"))
    val OR      = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b110"), funct7 = BitPat("b0000000"))
    val AND     = Instruction(opcode = BitPat("b0110011"), funct3 = BitPat("b111"), funct7 = BitPat("b0000000"))
    val FENCE   = Instruction(opcode = BitPat("b0001111"), funct3 = BitPat("b000"))
    val ECALL   = Instruction(
      opcode = BitPat("b1110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"),
      rs1 = BitPat("b00000"), rs2 = BitPat("b00000"), rd = BitPat("b00000")
    )
    val EBREAK  = Instruction(
      opcode = BitPat("b1110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"),
      rs1 = BitPat("b00000"), rs2 = BitPat("b00001"), rd = BitPat("b00000")
    )
    val CSRRW   = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b001"))
    val CSRRS   = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b010"))
    val CSRRC   = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b011"))
    val CSRRWI  = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b101"))
    val CSRRSI  = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b110"))
    val CSRRCI  = Instruction(opcode = BitPat("b1110011"), funct3 = BitPat("b111"))
  }

  val instrTypeMap = Map(
    BitPat("b0110111") -> InstructionType.UType,
    BitPat("b0010111") -> InstructionType.UType,
    BitPat("b1101111") -> InstructionType.JType,
    BitPat("b1100111") -> InstructionType.IType,
    BitPat("b1100011") -> InstructionType.BType,
    BitPat("b0000011") -> InstructionType.IType,
    BitPat("b0100011") -> InstructionType.SType,
    BitPat("b0010011") -> InstructionType.IType,
    BitPat("b0110011") -> InstructionType.RType,
    BitPat("b0001111") -> InstructionType.IType,
    BitPat("b1110011") -> InstructionType.IType,
    BitPat("b1110011") -> InstructionType.IType,
  )

  val immTypeMap = Map(
    InstructionType.IType -> ImmType.immI,
    InstructionType.SType -> ImmType.immS,
    InstructionType.BType -> ImmType.immB,
    InstructionType.UType -> ImmType.immU,
    InstructionType.JType -> ImmType.immJ,
  )

  val nop = Instruction(
    opcode = BitPat("b0010011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"),
    rs1 = BitPat("b00000"), rs2 = BitPat("b00000"), rd = BitPat("b00000")
  )
}

object ImmType {
  def getWidth = 3
  val immI = BitPat("b000")
  val immU = BitPat("b001")
  val immS = BitPat("b010")
  val immB = BitPat("b011")
  val immJ = BitPat("b100")
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

object BrType {
  def getWidth = 3
  val brNone  = BitPat("b000")
  val brJ     = BitPat("b001")
  val brJr    = BitPat("b010")
  val brEq    = BitPat("b100")
  val brNe    = BitPat("b101")
  val brLt    = BitPat("b110")
  val brGe    = BitPat("b111")
}

object MemOp {
  def getWidth = 3
  val memB  = BitPat("b000")
  val memH  = BitPat("b001")
  val memW  = BitPat("b010")
  val memBu = BitPat("b100")
  val memHu = BitPat("b101")
}

object ALUASrcFrom {
  def getWidth = 1
  val fromRs1 = BitPat("b0")
  val fromPc  = BitPat("b1")
}

object ALUBSrcFrom {
  def getWidth = 2
  val fromRs2 = BitPat("b00")
  val fromImm = BitPat("b01")
  val from4   = BitPat("b10")
}

object WBSrcFrom {
  def getWidth = 1
  val fromALU = BitPat("b0")
  val fromMem = BitPat("b1")
}

object CSRCtr {
  def getWidth = 2
  val csrNone = BitPat("b00")
  val csrRW   = BitPat("b01")
  val csrRS   = BitPat("b10")
  val csrRC   = BitPat("b11")
}
