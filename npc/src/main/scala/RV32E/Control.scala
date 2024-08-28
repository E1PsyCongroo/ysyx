package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import Instruction._

object ImmControlField extends DecodeField[Instruction, UInt] {
  def name = "Imm Control Field"
  def chiselType: UInt = UInt(ImmType.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    immTypeMap.getOrElse(instrTypeMap(op.opcode), dc)
  }
}

object RegWeControlField extends DecodeField[Instruction, Bool] {
  import InstructionType._
  def name = "Reg We Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case RType => BitPat.Y(1)
      case IType => BitPat.Y(1)
      case SType => BitPat.N(1)
      case BType => BitPat.N(1)
      case UType => BitPat.Y(1)
      case JType => BitPat.Y(1)
      case _ => dc
    }
  }
}

object ALUASrcControlField extends DecodeField[Instruction, UInt] {
  import InstricitonMap._
  import ALUASrcFrom._
  def name: String = "ALU Asrc Control Field"
  def chiselType: UInt = UInt(ALUASrcFrom.getWidth.W)
  override def default: BitPat = fromRs1
  def genTable(op: Instruction): BitPat = {
    op.opcode match {
      case AUIPC.opcode | JAL.opcode | JALR.opcode=> fromPc
      case _ => default
    }
  }
}

object ALUBSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionType._
  import InstricitonMap._
  import ALUBSrcFrom._
  def name: String = "ALU Bsrc Control Field"
  def chiselType: UInt = UInt(ALUBSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case RType | BType => fromRs2
      case IType => op.opcode match {
        case JALR.opcode => from4
        case _ => fromImm
      }
      case SType | UType => fromImm
      case JType => from4
      case _ => dc
    }
  }
}

object ALUControlField extends DecodeField[Instruction, UInt] {
  def name = "ALU Control Field"
  def chiselType: UInt = UInt(ALUOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    import InstricitonMap._
    import InstructionType._
    import ALUOp._
    instrTypeMap(op.opcode) match {
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
        case SRL.funct3 | SRA.funct3 => op.funct7 match {
          case SRL.funct7 => aluSrl
          case SRA.funct7 => aluSra
          case _ =>dc
        }
        case OR.funct3 => aluOr
        case AND.funct3 => aluAnd
        case _ => dc
      }
      case IType => op.opcode match {
        case JALR.opcode => aluAdd
        case LB.opcode | LH.opcode | LW.opcode | LBU.opcode | LHU.opcode => aluAdd
        case ADDI.opcode | SLLI.opcode | SLTI.opcode | SLTIU.opcode |
             XORI.opcode | SRLI.opcode | SRAI.opcode | ORI.opcode |
             ANDI.opcode => op.funct3 match {
          case ADDI.funct3 => aluAdd
          case SLLI.funct3 => aluSll
          case SLTI.funct3 => aluSlt
          case SLTIU.funct3 => aluSltu
          case XORI.funct3 => aluXor
          case SRLI.funct3 | SRAI.funct3 => op.funct7 match {
            case SRLI.funct7 => aluSrl
            case SRAI.funct7 => aluSra
            case _ => dc
          }
          case ORI.funct3 => aluOr
          case ANDI.funct3 => aluAnd
          case _ => dc
        }
        case _ => dc
      }
      case SType => aluAdd
      case BType => op.funct3 match {
        case BEQ.funct3 | BNE.funct3 => aluSub
        case BLT.funct3 | BGE.funct3 => aluSlt
        case BLTU.funct3 | BGEU.funct3 => aluSltu
        case _ => dc
      }
      case UType => op.opcode match {
        case LUI.opcode => aluBout
        case AUIPC.opcode => aluAdd
        case _ => dc
      }
      case JType => aluAdd
    }
  }
}

object CSRSrcControlField extends DecodeField[Instruction, UInt] {
  import InstricitonMap._
  import CSRSrcFrom._
  def name: String = "CSR Src Control Field"
  def chiselType: UInt = UInt(CSRSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op.funct3 match {
      case CSRRW.funct3 | CSRRS.funct3 | CSRRC.funct3 => fromRs1
      case CSRRWI.funct3 | CSRRSI.funct3 | CSRRCI.funct3 => fromUimm
      case _ => dc
    }
  }
}

object CSRControlField extends DecodeField[Instruction, UInt] {
  import InstricitonMap._
  import CSRCtr._
  def name: String = "CSR Control Field"
  def chiselType: UInt = UInt(CSRCtr.getWidth.W)
  override def default: BitPat = csrNone
  def genTable(op: Instruction): BitPat = {
    op match {
      case ECALL => csrEcall
      case MRET => csrMret
      case CSRRW | CSRRWI => csrRW
      case CSRRS | CSRRSI => csrRS
      case CSRRC | CSRRCI => csrRC
      case _ => default
    }
  }
}

object BrControlField extends DecodeField[Instruction, UInt] {
  import BrType._
  import InstricitonMap._
  import InstructionType._
  def name: String = "Branch Control Field"
  def chiselType: UInt = UInt(BrType.getWidth.W)
  override def default: BitPat = brNone
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case JType => op.opcode match {
        case JAL.opcode => brJ
        case _ => default
      }
      case IType => op.opcode match {
        case JALR.opcode => brJr
        case _ => default
      }
      case BType => op.funct3 match {
        case BEQ.funct3 => brEq
        case BNE.funct3 => brNe
        case BLT.funct3 | BLTU.funct3 => brLt
        case BGE.funct3 | BGEU.funct3 => brGe
        case _ => default
      }
      case _ => default
    }
  }
}

object MemValidControlField extends DecodeField[Instruction, Bool] {
  import InstricitonMap._
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
  import InstricitonMap._
  import InstructionType._
  import MemOp._
  def name: String = "MemOp Control Field"
  def chiselType: UInt = UInt(MemOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
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
    instrTypeMap(op.opcode) match {
      case InstructionType.SType => BitPat.Y(1)
      case _ => BitPat.N(1)
    }
  }
}
object PCSrcControlField extends DecodeField[Instruction, UInt] {
  import InstricitonMap._
  import PCSrcFrom._
  def name: String = "PC Select Control Feild"
  def chiselType: UInt = UInt(PCSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op match {
      case ECALL | MRET => fromCSR
      case _ => fromCom
    }
  }
}

object WBSrcControlField extends DecodeField[Instruction, UInt] {
  import WBSrcFrom._
  import InstricitonMap._
  def name: String = "Write Back Control Feild"
  def chiselType: UInt = UInt(WBSrcFrom.getWidth.W)
  override def default: BitPat = fromALU
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | LBU | LH | LHU | LW => fromMem
      case CSRRW | CSRRWI | CSRRS | CSRRSI | CSRRC | CSRRCI => fromCSR
      case _ => default
    }
  }
}

object EndControlField extends DecodeField[Instruction, Bool] {
  import InstricitonMap._
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
  val instruction = Input(UInt(32.W))
  val immType = Output(UInt(ImmType.getWidth.W))
  val regWe = Output(Bool())
  val aluASrc = Output(UInt(ALUASrcFrom.getWidth.W))
  val aluBSrc = Output(UInt(ALUBSrcFrom.getWidth.W))
  val aluCtr = Output(UInt(ALUOp.getWidth.W))
  val csrSrc = Output(UInt(CSRSrcFrom.getWidth.W))
  val csrCtr = Output(UInt(CSRCtr.getWidth.W))
  val brType = Output(UInt(BrType.getWidth.W))
  val pcSrc = Output(UInt(PCSrcFrom.getWidth.W))
  val wbSrc = Output(UInt(WBSrcFrom.getWidth.W))
  val memValid = Output(Bool())
  val memWe = Output(Bool())
  val memOp = Output(UInt(MemOp.getWidth.W))
  val isEnd = Output(Bool())
}

class Control extends Module {
  val io = IO(new ControlIO)
  import InstricitonMap._

  val possiblePatterns = Seq(
    LUI, AUIPC, JAL, JALR, BEQ, BNE, BLT, BGE, BLTU, BGEU,
    LB, LH, LW, LBU, LHU, SB, SH, SW, ADDI, SLTI, SLTIU,
    XORI, ORI, ANDI, SLLI, SRLI, SRAI, ADD, SUB, SLL, SLT,
    SLTU, XOR, SRL, SRA, OR, AND, FENCE, ECALL, EBREAK,
    CSRRW, CSRRS, CSRRC, CSRRWI, CSRRSI, CSRRCI, MRET
  )
  val decodeTable = new DecodeTable(
    possiblePatterns,
    Seq(
      ImmControlField,
      RegWeControlField,
      ALUASrcControlField,
      ALUBSrcControlField,
      ALUControlField,
      CSRSrcControlField,
      CSRControlField,
      BrControlField,
      MemValidControlField,
      MemOpControlField,
      MemWenControlField,
      PCSrcControlField,
      WBSrcControlField,
      EndControlField,
    )
  )
  val decodeResult = decodeTable.decode(io.instruction)
  io.immType      := decodeResult(ImmControlField)
  io.regWe        := decodeResult(RegWeControlField)
  io.aluASrc      := decodeResult(ALUASrcControlField)
  io.aluBSrc      := decodeResult(ALUBSrcControlField)
  io.aluCtr       := decodeResult(ALUControlField)
  io.csrSrc       := decodeResult(CSRSrcControlField)
  io.csrCtr       := decodeResult(CSRControlField)
  io.brType       := decodeResult(BrControlField)
  io.pcSrc        := decodeResult(PCSrcControlField)
  io.wbSrc        := decodeResult(WBSrcControlField)
  io.memValid     := decodeResult(MemValidControlField)
  io.memWe        := decodeResult(MemWenControlField)
  io.memOp        := decodeResult(MemOpControlField)
  io.isEnd        := decodeResult(EndControlField)
}
