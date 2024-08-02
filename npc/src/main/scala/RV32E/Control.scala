package RV32E

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

case class ControlPattern(
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
  object InstricitonMap {
    val LUI     = ControlPattern(opcode = BitPat("b0110111"))
    val AUIPC   = ControlPattern(opcode = BitPat("b0010111"))
    val JAL     = ControlPattern(opcode = BitPat("b1101111"))
    val JALR    = ControlPattern(opcode = BitPat("b1100111"), funct3 = BitPat("b000"))
    val BEQ     = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b000"))
    val BNE     = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b001"))
    val BLT     = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b100"))
    val BGE     = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b101"))
    val BLTU    = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b110"))
    val BGEU    = ControlPattern(opcode = BitPat("b1100011"), funct3 = BitPat("b111"))
    val LB      = ControlPattern(opcode = BitPat("b0000011"), funct3 = BitPat("b000"))
    val LH      = ControlPattern(opcode = BitPat("b0000011"), funct3 = BitPat("b001"))
    val LW      = ControlPattern(opcode = BitPat("b0000011"), funct3 = BitPat("b010"))
    val LBU     = ControlPattern(opcode = BitPat("b0000011"), funct3 = BitPat("b100"))
    val LHU     = ControlPattern(opcode = BitPat("b0000011"), funct3 = BitPat("b101"))
    val SB      = ControlPattern(opcode = BitPat("b0100011"), funct3 = BitPat("b000"))
    val SH      = ControlPattern(opcode = BitPat("b0100011"), funct3 = BitPat("b001"))
    val SW      = ControlPattern(opcode = BitPat("b0100011"), funct3 = BitPat("b010"))
    val ADDI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b000"))
    val SLTI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b010"))
    val SLTIU   = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b011"))
    val XORI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b100"))
    val ORI     = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b110"))
    val ANDI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b111"))
    val SLLI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b001"), funct7 = BitPat("b0000000"))
    val SRLI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b101"), funct7 = BitPat("b0000000"))
    val SRAI    = ControlPattern(opcode = BitPat("b0010011"), funct3 = BitPat("b101"), funct7 = BitPat("b0100000"))
    val ADD     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"))
    val SUB     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0100000"))
    val SLL     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b001"), funct7 = BitPat("b0000000"))
    val SLT     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b010"), funct7 = BitPat("b0000000"))
    val SLTU    = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b011"), funct7 = BitPat("b0000000"))
    val XOR     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b100"), funct7 = BitPat("b0000000"))
    val SRL     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b101"), funct7 = BitPat("b0000000"))
    val SRA     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b101"), funct7 = BitPat("b0100000"))
    val OR      = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b110"), funct7 = BitPat("b0000000"))
    val AND     = ControlPattern(opcode = BitPat("b0110011"), funct3 = BitPat("b111"), funct7 = BitPat("b0000000"))
    val FENCE   = ControlPattern(opcode = BitPat("b0001111"), funct3 = BitPat("b000"))
    val ECALL   = ControlPattern(
      opcode = BitPat("b1110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"),
      rs2 = BitPat("b00000"), rs1 = BitPat("b00000"), rd = BitPat("b00000")
    )
    val EBREAK  = ControlPattern(
      opcode = BitPat("b1110011"), funct3 = BitPat("b000"), funct7 = BitPat("b0000000"),
      rs2 = BitPat("b00001"), rs1 = BitPat("b00000"), rd = BitPat("b00000")
    )
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
  )

  val immTypeMap = Map(
    InstructionType.IType -> ImmType.ImmI,
    InstructionType.SType -> ImmType.ImmS,
    InstructionType.BType -> ImmType.ImmB,
    InstructionType.UType -> ImmType.ImmU,
    InstructionType.JType -> ImmType.ImmJ,
  )
}

object ImmControlField extends DecodeField[ControlPattern, UInt] {
  def name = "Imm Control Field"
  def chiselType: UInt = UInt(ImmType.getWidth.W)
  def genTable(op: ControlPattern): BitPat = {
    Instruction.immTypeMap.get(Instruction.instrTypeMap(op.opcode)) match {
      case Some(immType) => BitPat(immType.litValue.U(width.W))
      case None => dc
    }
  }
}

object RegWeControlField extends DecodeField[ControlPattern, Bool] {
  def name = "Reg We Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case InstructionType.RType => BitPat.Y(1)
      case InstructionType.IType => BitPat.Y(1)
      case InstructionType.SType => BitPat.N(1)
      case InstructionType.BType => BitPat.N(1)
      case InstructionType.UType => BitPat.Y(1)
      case InstructionType.JType => BitPat.Y(1)
      case _ => dc
    }
  }
}

object ALUControlField extends DecodeField[ControlPattern, UInt] {
  def name = "ALU Control Field"
  def chiselType: UInt = UInt(ALUOp.getWidth.W)
  def genTable(op: ControlPattern): BitPat = {
    import Instruction.InstricitonMap._
    import InstructionType._
    import ALUOp._
    Instruction.instrTypeMap(op.opcode) match {
      case RType => op.funct3 match {
        case ADD.funct3 => op.funct7 match {
          case ADD.funct7 => aluAdd
          case SUB.funct7 => aluSub
          case _ => dc
        }
        case SLL.funct3 => aluSll
        case SLT.funct3 => aluSlt
        case SLTU.funct3 => aluSltu
        case XOR.funct3 => aluXor
        case SRL.funct3 => aluSrl
        case SRA.funct3 => aluSra
        case OR.funct3 => aluOr
        case AND.funct3 => aluAnd
        case _ => dc
      }
      case IType => op.funct3 match {
        case ADDI.funct3 => aluAdd
        case SLLI.funct3 => aluSll
        case SLTI.funct3 => aluSlt
        case SLTIU.funct3 => aluSltu
        case XORI.funct3 => aluXor
        case SRLI.funct3 => aluSrl
        case SRAI.funct3 => aluSra
        case ORI.funct3 => aluOr
        case ANDI.funct3 => aluAnd
        case _ => dc
      }
      case SType => aluAdd
      case BType => aluSub
      case UType => op.opcode match {
        case LUI.opcode => aluBout
        case AUIPC.opcode => aluAdd
        case _ => dc
      }
      case JType => aluAdd
    }
  }
}

object BrControlField extends DecodeField[ControlPattern, UInt] {
  import Instruction.InstricitonMap._
  import BrType._
  def name: String = "Branch Control Field"
  def chiselType: UInt = UInt(BrType.getWidth.W)
  override def default: BitPat = BitPat(brNone.litValue.U(width.W))
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case InstructionType.JType => op.opcode match {
        case JAL.opcode => BitPat(brJ.litValue.U(width.W))
        case JALR.opcode => BitPat(brJr.litValue.U(width.W))
        case _ => default
      }
      case InstructionType.BType => op.funct3 match {
        case BEQ.funct3 => BitPat(brEq.litValue.U(width.W))
        case BNE.funct3 => BitPat(brNe.litValue.U(width.W))
        case BLT.funct3 | BLTU.funct3 => BitPat(brLt.litValue.U(width.W))
        case BGE.funct3 | BGEU.funct3 => BitPat(brGe.litValue.U(width.W))
        case _ => default
      }
      case _ => default
    }
  }
}

object MemOpControlField extends DecodeField[ControlPattern, UInt] {
  import Instruction.InstricitonMap._
  import InstructionType._
  import MemOp._
  def name: String = "MemOp Control Field"
  def chiselType: UInt = UInt(MemOp.getWidth.W)
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case IType => op.funct3 match {
        case LB.funct3 => memB
        case LH.funct3 => memH
        case LW.funct3 => memW
        case LBU.funct3 => memBu
        case LHU.funct3 => memHu
        case _ => dc
      }
      case SType => op.funct3 match {
        case SB.funct3 => memB
        case SH.funct3 => memH
        case SW.funct3 => memW
        case _ => dc
      }
      case _ => dc
    }
  }
  // def genTable(op: ControlPattern): BitPat = {
  //   op.funct3
  // }
}

object MemWenControlField extends DecodeField[ControlPattern, Bool] {
  def name: String = "MemWen Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case InstructionType.SType => BitPat.Y(1)
      case _ => BitPat.N(1)
    }
  }
}

object ALUASrcControlField extends DecodeField[ControlPattern, UInt] {
  import Instruction.InstricitonMap._
  import ALUASrcFrom._
  def name: String = "ALU Asrc Control Field"
  def chiselType: UInt = UInt(ALUASrcFrom.getWidth.W)
  override def default: BitPat = BitPat(fromRs1.litValue.U(width.W))
  def genTable(op: ControlPattern): BitPat = {
    op.opcode match {
      case AUIPC.opcode => BitPat(fromPc.litValue.U(width.W))
      case JAL.opcode => BitPat(fromPc.litValue.U(width.W))
      case JALR.opcode => BitPat(fromPc.litValue.U(width.W))
      case _ => default
    }
  }
}

object ALUBSrcControlField extends DecodeField[ControlPattern, UInt] {
  import InstructionType._
  import ALUBSrcFrom._
  def name: String = "ALU Bsrc Control Field"
  def chiselType: UInt = UInt(ALUBSrcFrom.getWidth.W)
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case RType | BType => BitPat(fromRs2.litValue.U(width.W))
      case IType => op.opcode match {
        case Instruction.InstricitonMap.JALR => BitPat(from4.litValue.U(width.W))
        case _ => BitPat(fromImm.litValue.U(width.W))
      }
      case SType | UType => BitPat(fromImm.litValue.U(width.W))
      case JType => BitPat(from4.litValue.U(width.W))
      case _ => dc
    }
  }
}

object WBSrcControlField extends DecodeField[ControlPattern, UInt] {
  import WBSrcFrom._
  def name: String = "Write Back Control Feild"
  def chiselType: UInt = UInt(WBSrcFrom.getWidth.W)
  override def default: BitPat = BitPat(fromALU.litValue.U(width.W))
  def genTable(op: ControlPattern): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case InstructionType.SType => BitPat(fromMem.litValue.U(width.W))
      case _ => default
    }
  }
}

class ControlIO extends Bundle {
  val instr = Input(UInt(32.W))
  val immType = Output(UInt(ImmType.getWidth.W))
  val regWe = Output(Bool())
  val aluASrc = Output(UInt(ALUASrcFrom.getWidth.W))
  val aluBSrc = Output(UInt(ALUBSrcFrom.getWidth.W))
  val aluCtr = Output(UInt(ALUOp.getWidth.W))
  val brType = Output(UInt(BrType.getWidth.W))
  val wbSrc = Output(UInt(WBSrcFrom.getWidth.W))
  val memWe = Output(Bool())
  val memOp = Output(UInt(MemOp.getWidth.W))
}

class Control extends Module {
  val io = IO(new ControlIO)
  import Instruction.InstricitonMap._

  val possiblePatterns = Seq(
    LUI, AUIPC, JAL, JALR, BEQ, BNE, BLT, BGE, BLTU, BGEU,
    LB, LH, LW, LBU, LHU, SB, SH, SW, ADDI, SLTI, SLTIU,
    XORI, ORI, ANDI, SLLI, SRLI, SRAI, ADD, SUB, SLL, SLT,
    SLTU, XOR, SRL, SRA, OR, AND, FENCE, ECALL, EBREAK
  )
  val decodeTable = new DecodeTable(
    possiblePatterns,
    Seq(
      ImmControlField,
      RegWeControlField,
      ALUControlField,
      BrControlField,
      MemOpControlField,
      MemWenControlField,
      ALUASrcControlField,
      ALUBSrcControlField,
      WBSrcControlField
    )
  )
  val decodeResult = decodeTable.decode(io.instr)
  io.immType      := decodeResult(ImmControlField)
  io.regWe        := decodeResult(RegWeControlField)
  io.aluASrc      := decodeResult(ALUASrcControlField)
  io.aluBSrc      := decodeResult(ALUBSrcControlField)
  io.aluCtr       := decodeResult(ALUControlField)
  io.brType       := decodeResult(BrControlField)
  io.wbSrc        := decodeResult(WBSrcControlField)
  io.memWe        := decodeResult(MemWenControlField)
  io.memOp        := decodeResult(MemOpControlField)
}
