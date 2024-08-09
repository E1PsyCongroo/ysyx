package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import Instruction.InstructionType

object ImmControlField extends DecodeField[Instruction, UInt] {
  def name = "Imm Control Field"
  def chiselType: UInt = UInt(ImmType.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    Instruction.immTypeMap.get(Instruction.instrTypeMap(op.opcode)) match {
      case Some(immType) => BitPat(immType.litValue.U(width.W))
      case None => dc
    }
  }
}

object RegWeControlField extends DecodeField[Instruction, Bool] {
  def name = "Reg We Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
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

object ALUControlField extends DecodeField[Instruction, UInt] {
  def name = "ALU Control Field"
  def chiselType: UInt = UInt(ALUOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
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

object BrControlField extends DecodeField[Instruction, UInt] {
  import Instruction.InstricitonMap._
  import BrType._
  def name: String = "Branch Control Field"
  def chiselType: UInt = UInt(BrType.getWidth.W)
  override def default: BitPat = BitPat(brNone.litValue.U(width.W))
  def genTable(op: Instruction): BitPat = {
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

object MemValidControlField extends DecodeField[Instruction, Bool] {
  import Instruction.InstricitonMap._
  def name: String = " Mem Valid Control Field"
  def chiselType: Bool = Bool()
  override def default: BitPat = BitPat.N(1)
  def genTable(op: Instruction): BitPat = {
    op.opcode match {
      case LB.opcode => BitPat.Y(1)
      case LBU.opcode => BitPat.Y(1)
      case LH.opcode => BitPat.Y(1)
      case LHU.opcode => BitPat.Y(1)
      case LW.opcode => BitPat.Y(1)
      case SB.opcode => BitPat.Y(1)
      case SH.opcode => BitPat.Y(1)
      case SW.opcode => BitPat.Y(1)
      case _ => default
    }
  }
}

object MemOpControlField extends DecodeField[Instruction, UInt] {
  import Instruction.InstricitonMap._
  import InstructionType._
  import MemOp._
  def name: String = "MemOp Control Field"
  def chiselType: UInt = UInt(MemOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
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
}

object MemWenControlField extends DecodeField[Instruction, Bool] {
  def name: String = "Mem Wen Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
    Instruction.instrTypeMap(op.opcode) match {
      case InstructionType.SType => BitPat.Y(1)
      case _ => BitPat.N(1)
    }
  }
}

object ALUASrcControlField extends DecodeField[Instruction, UInt] {
  import Instruction.InstricitonMap._
  import ALUASrcFrom._
  def name: String = "ALU Asrc Control Field"
  def chiselType: UInt = UInt(ALUASrcFrom.getWidth.W)
  override def default: BitPat = BitPat(fromRs1.litValue.U(width.W))
  def genTable(op: Instruction): BitPat = {
    op.opcode match {
      case AUIPC.opcode => BitPat(fromPc.litValue.U(width.W))
      case JAL.opcode => BitPat(fromPc.litValue.U(width.W))
      case JALR.opcode => BitPat(fromPc.litValue.U(width.W))
      case _ => default
    }
  }
}

object ALUBSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionType._
  import ALUBSrcFrom._
  def name: String = "ALU Bsrc Control Field"
  def chiselType: UInt = UInt(ALUBSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
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

object WBSrcControlField extends DecodeField[Instruction, UInt] {
  import WBSrcFrom._
  import Instruction.InstricitonMap._
  def name: String = "Write Back Control Feild"
  def chiselType: UInt = UInt(WBSrcFrom.getWidth.W)
  override def default: BitPat = BitPat(fromALU.litValue.U(width.W))
  def genTable(op: Instruction): BitPat = {
    op.opcode match {
      case LB.opcode => BitPat(fromMem.litValue.U(width.W))
      case LBU.opcode => BitPat(fromMem.litValue.U(width.W))
      case LH.opcode => BitPat(fromMem.litValue.U(width.W))
      case LHU.opcode => BitPat(fromMem.litValue.U(width.W))
      case LW.opcode => BitPat(fromMem.litValue.U(width.W))
      case _ => default
    }
  }
}

object EndControlField extends DecodeField[Instruction, Bool] {
  import Instruction.InstricitonMap._
  def name: String = "End Control Feild"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
    op match {
      case EBREAK => BitPat.Y(1)
      case _ => BitPat.N(1)
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
  val memValid = Output(Bool())
  val memWe = Output(Bool())
  val memOp = Output(UInt(MemOp.getWidth.W))
  val isEnd = Output(Bool())
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
      MemValidControlField,
      MemOpControlField,
      MemWenControlField,
      ALUASrcControlField,
      ALUBSrcControlField,
      WBSrcControlField,
      EndControlField,
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
  io.memValid     := decodeResult(MemValidControlField)
  io.memWe        := decodeResult(MemWenControlField)
  io.memOp        := decodeResult(MemOpControlField)
  io.isEnd        := decodeResult(EndControlField)
}
