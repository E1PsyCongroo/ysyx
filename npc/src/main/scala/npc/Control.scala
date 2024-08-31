package rvcpu.core

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
    op match {
      case AUIPC | JAL | JALR => fromPc
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
      case RType => op match {
        case ADD => aluAdd
        case SUB => aluSub
        case SLL => aluSll
        case SLT => aluSlt
        case SLTU => aluSltu
        case XOR => aluXor
        case SRL => aluSrl
        case SRA => aluSra
        case OR => aluOr
        case AND => aluAnd
        case _ => dc
      }
      case IType => op match {
        case JALR => aluAdd
        case LB | LH | LW | LBU | LHU => aluAdd
        case ADDI => aluAdd
        case SLLI => aluSll
        case SLTI => aluSlt
        case SLTIU => aluSltu
        case XORI => aluXor
        case SRLI => aluSrl
        case SRAI => aluSra
        case ORI => aluOr
        case ANDI => aluAnd
        case _ => dc
      }
      case SType => aluAdd
      case BType => op match {
        case BEQ | BNE => aluSub
        case BLT | BGE => aluSlt
        case BLTU | BGEU => aluSltu
        case _ => dc
      }
      case UType => op match {
        case LUI => aluBout
        case AUIPC => aluAdd
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
    op match {
      case CSRRW | CSRRS | CSRRC => fromRs1
      case CSRRWI | CSRRSI | CSRRCI => fromUimm
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
    op match {
      case JAL => brJ
      case JALR => brJr
      case BEQ => brEq
      case BNE => brNe
      case BLT | BLTU => brLt
      case BGE | BGEU => brGe
      case _ => default
    }
  }
}

object MemRenControlField extends DecodeField[Instruction, Bool] {
  import InstricitonMap._
  def name: String = " Mem Valid Control Field"
  def chiselType: Bool = Bool()
  override def default: BitPat = BitPat.N(1)
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | LBU | LH | LHU | LW => BitPat.Y(1)
      case _ => default
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

object MemOpControlField extends DecodeField[Instruction, UInt] {
  import InstricitonMap._
  import InstructionType._
  import MemOp._
  def name: String = "MemOp Control Field"
  def chiselType: UInt = UInt(MemOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | SB => memB
      case LH | SH => memH
      case LW | SW => memW
      case LBU => memBu
      case LHU => memHu
      case _ => dc
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
  val immType     = Output(UInt(ImmType.getWidth.W))
  val regWe       = Output(Bool())
  val aluASrc     = Output(UInt(ALUASrcFrom.getWidth.W))
  val aluBSrc     = Output(UInt(ALUBSrcFrom.getWidth.W))
  val aluCtr      = Output(UInt(ALUOp.getWidth.W))
  val csrSrc      = Output(UInt(CSRSrcFrom.getWidth.W))
  val csrCtr      = Output(UInt(CSRCtr.getWidth.W))
  val brType      = Output(UInt(BrType.getWidth.W))
  val pcSrc       = Output(UInt(PCSrcFrom.getWidth.W))
  val wbSrc       = Output(UInt(WBSrcFrom.getWidth.W))
  val memRen      = Output(Bool())
  val memWen      = Output(Bool())
  val memOp       = Output(UInt(MemOp.getWidth.W))
  val isEnd       = Output(Bool())
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
      MemRenControlField,
      MemWenControlField,
      MemOpControlField,
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
  io.memRen       := decodeResult(MemRenControlField)
  io.memWen       := decodeResult(MemWenControlField)
  io.memOp        := decodeResult(MemOpControlField)
  io.isEnd        := decodeResult(EndControlField)
}
