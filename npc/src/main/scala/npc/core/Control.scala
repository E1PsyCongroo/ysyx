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

object needRd1ControlField extends DecodeField[Instruction, Bool] {
  import InstructionType._
  import InstructionMap._
  def name = "Reg Need Rd1 Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case RType | SType | BType => BitPat.Y(1)
      case UType | JType         => BitPat.N(1)
      case IType =>
        op match {
          case CSRRWI | CSRRSI | CSRRCI | ECALL | EBREAK => BitPat.N(1)
          case _                                         => BitPat.Y(1)
        }
      case _ => dc
    }
  }
}

object needRd2ControlField extends DecodeField[Instruction, Bool] {
  import InstructionType._
  def name = "Reg Need Rd2 Control Field"
  def chiselType: Bool = Bool()
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case RType | SType | BType => BitPat.Y(1)
      case IType | UType | JType => BitPat.N(1)
      case _                     => dc
    }
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
      case _     => dc
    }
  }
}

object WBSrcControlField extends DecodeField[Instruction, UInt] {
  import WBSrcFrom._
  import InstructionMap._
  def name:             String = "Write Back Control Feild"
  def chiselType:       UInt   = UInt(WBSrcFrom.getWidth.W)
  override def default: BitPat = fromALU
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | LBU | LH | LHU | LW                         => fromMem
      case CSRRW | CSRRWI | CSRRS | CSRRSI | CSRRC | CSRRCI => fromCSR
      case _                                                => default
    }
  }
}

object ALUASrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import ALUASrcFrom._
  def name:             String = "ALU Asrc Control Field"
  def chiselType:       UInt   = UInt(ALUASrcFrom.getWidth.W)
  override def default: BitPat = fromRs1
  def genTable(op: Instruction): BitPat = {
    op match {
      case AUIPC | JAL | JALR => fromPc
      case _                  => default
    }
  }
}

object ALUBSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionType._
  import InstructionMap._
  import ALUBSrcFrom._
  def name:       String = "ALU Bsrc Control Field"
  def chiselType: UInt   = UInt(ALUBSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case RType | BType => fromRs2
      case IType =>
        op.opcode match {
          case JALR.opcode => from4
          case _           => fromImm
        }
      case SType | UType => fromImm
      case JType         => from4
      case _             => dc
    }
  }
}

class ALUControlOutput extends Bundle {
  val aluSel     = Output(UInt(ALUOutSel.getWidth.W))
  val isArith    = Output(Bool())
  val isLeft     = Output(Bool())
  val isUnsigned = Output(Bool())
  val isSub      = Output(Bool())
}

object ALUControlField extends DecodeField[Instruction, ALUControlOutput] {
  def name = "ALU Control Field"
  def chiselType: ALUControlOutput = new ALUControlOutput
  def genTable(op: Instruction): BitPat = {
    import InstructionMap._
    import InstructionType._
    import ALUOutSel._
    val aluSel = op match {
      case ADD | ADDI | SUB | JAL | JALR | AUIPC            => selectAdder
      case LB | LH | LW | LBU | LHU                         => selectAdder
      case SH | SB | SW                                     => selectAdder
      case SLL | SLLI | SRL | SRLI | SRA | SRAI             => selectShift
      case SLT | SLTU | BEQ | BNE | BLT | BGE | BLTU | BGEU => selectSlt
      case LUI                                              => selectB
      case XOR | XORI                                       => selectXor
      case OR | ORI                                         => selectOr
      case AND | ANDI                                       => selectAnd
      case _                                                => BitPat.dontCare(ALUOutSel.getWidth)
    }
    val isArith = op match {
      case SRL | SRLI => BitPat.N(1)
      case SRA | SRAI => BitPat.Y(1)
      case _          => BitPat.dontCare(1)
    }
    val isLeft = op match {
      case SRL | SRLI | SRA | SRAI => BitPat.N(1)
      case SLL | SLLI              => BitPat.Y(1)
      case _                       => BitPat.dontCare(1)
    }
    val isUnsigned = op match {
      case SLT | SLTI | BLT | BGE     => BitPat.N(1)
      case SLTU | SLTIU | BLTU | BGEU => BitPat.Y(1)
      case _                          => BitPat.dontCare(1)
    }
    val isSub = op match {
      case ADD | ADDI | JAL | JALR             => BitPat.N(1)
      case LB | LH | LW | LBU | LHU            => BitPat.N(1)
      case SB | SH | SW                        => BitPat.N(1)
      case SUB | SLT | SLTI | SLTU | SLTIU     => BitPat.Y(1)
      case BEQ | BNE | BLT | BLTU | BGE | BGEU => BitPat.Y(1)
      case _                                   => BitPat.dontCare(1)
    }
    aluSel ## isArith ## isLeft ## isUnsigned ## isSub
  }
}

object CSRSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import CSRSrcFrom._
  def name:       String = "CSR Src Control Field"
  def chiselType: UInt   = UInt(CSRSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op match {
      case CSRRW | CSRRS | CSRRC    => fromRs1
      case CSRRWI | CSRRSI | CSRRCI => fromUimm
      case _                        => dc
    }
  }
}

object CSRControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import CSRCtr._
  def name:             String = "CSR Control Field"
  def chiselType:       UInt   = UInt(CSRCtr.getWidth.W)
  override def default: BitPat = csrNone
  def genTable(op: Instruction): BitPat = {
    op match {
      case ECALL          => csrExcept
      case MRET           => csrMret
      case CSRRW | CSRRWI => csrRW
      case CSRRS | CSRRSI => csrRS
      case CSRRC | CSRRCI => csrRC
      case _              => default
    }
  }
}

object JumpASrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import InstructionType._
  import JumpASrcFrom._
  def name:       String = "Jump ASrc Control Field"
  def chiselType: UInt   = UInt(JumpASrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case BType | JType => fromPc
      case _ =>
        op match {
          case JALR                                             => fromRs1
          case CSRRW | CSRRWI | CSRRS | CSRRSI | CSRRC | CSRRCI => fromPc
          case _                                                => dc
        }
    }
  }
}

object JumpBSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import InstructionType._
  import JumpBSrcFrom._
  def name:       String = "Jump BSrc Control Field"
  def chiselType: UInt   = UInt(JumpBSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case BType | JType => fromImm
      case _ =>
        op match {
          case SB | SH | SW => fromRs2
          case JALR         => fromImm
          case _            => dc
        }
    }
  }
}

object BrControlField extends DecodeField[Instruction, UInt] {
  import BrType._
  import InstructionMap._
  import InstructionType._
  def name:             String = "Branch Control Field"
  def chiselType:       UInt   = UInt(BrType.getWidth.W)
  override def default: BitPat = brNone
  def genTable(op: Instruction): BitPat = {
    op match {
      case JAL | JALR => brJ
      case BEQ        => brEq
      case BNE        => brNe
      case BLT | BLTU => brLt
      case BGE | BGEU => brGe
      case _          => default
    }
  }
}

object PCSrcControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import PCSrcFrom._
  def name:       String = "PC Select Control Feild"
  def chiselType: UInt   = UInt(PCSrcFrom.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op match {
      case ECALL | MRET => fromCSR
      case _            => fromCom
    }
  }
}

object MemRenControlField extends DecodeField[Instruction, Bool] {
  import InstructionMap._
  def name:             String = " Mem Valid Control Field"
  def chiselType:       Bool   = Bool()
  override def default: BitPat = BitPat.N(1)
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | LBU | LH | LHU | LW => BitPat.Y(1)
      case _                        => default
    }
  }
}

object MemWenControlField extends DecodeField[Instruction, Bool] {
  def name:       String = "Mem Wen Control Field"
  def chiselType: Bool   = Bool()
  def genTable(op: Instruction): BitPat = {
    instrTypeMap(op.opcode) match {
      case InstructionType.SType => BitPat.Y(1)
      case _                     => BitPat.N(1)
    }
  }
}

object MemOpControlField extends DecodeField[Instruction, UInt] {
  import InstructionMap._
  import InstructionType._
  import MemOp._
  def name:       String = "MemOp Control Field"
  def chiselType: UInt   = UInt(MemOp.getWidth.W)
  def genTable(op: Instruction): BitPat = {
    op match {
      case LB | SB => memB
      case LH | SH => memH
      case LW | SW => memW
      case LBU     => memBu
      case LHU     => memHu
      case _       => dc
    }
  }
}

object FENCE_IField extends DecodeField[Instruction, Bool] {
  import InstructionMap._
  def name:             String = "fence_i Feild"
  def chiselType:       Bool   = Bool()
  override def default: BitPat = BitPat.N(1)
  def genTable(op: Instruction): BitPat = {
    op match {
      case FENCE_I => BitPat.Y(1)
      case _       => default
    }
  }
}

object EndControlField extends DecodeField[Instruction, Bool] {
  import InstructionMap._
  def name:       String = "End Control Feild"
  def chiselType: Bool   = Bool()
  def genTable(op: Instruction): BitPat = {
    op match {
      case EBREAK => BitPat.Y(1)
      case _      => BitPat.N(1)
    }
  }
}

class ControlOut(xlen: Int, sim: Boolean) extends Bundle {
  val immType = Output(UInt(ImmType.getWidth.W))

  val needRd1 = Output(Bool())
  val needRd2 = Output(Bool())
  val regWe   = Output(Bool())
  val wbSrc   = Output(UInt(WBSrcFrom.getWidth.W))

  val aluASrc    = Output(UInt(ALUASrcFrom.getWidth.W))
  val aluBSrc    = Output(UInt(ALUBSrcFrom.getWidth.W))
  val aluSel     = Output(UInt(ALUOutSel.getWidth.W))
  val isArith    = Output(Bool())
  val isLeft     = Output(Bool())
  val isUnsigned = Output(Bool())
  val isSub      = Output(Bool())

  val csrSrc = Output(UInt(CSRSrcFrom.getWidth.W))
  val csrCtr = Output(UInt(CSRCtr.getWidth.W))

  val jumpASrc = Output(UInt(JumpASrcFrom.getWidth.W))
  val jumpBSrc = Output(UInt(JumpBSrcFrom.getWidth.W))
  val brType   = Output(UInt(BrType.getWidth.W))
  val pcSrc    = Output(UInt(PCSrcFrom.getWidth.W))

  val memRen = Output(Bool())
  val memWen = Output(Bool())
  val memOp  = Output(UInt(MemOp.getWidth.W))

  val fence_i = Output(Bool())
  val isEnd   = if (sim) Some(Output(Bool())) else None
}

object Control {
  def apply(instruct: UInt, xlen: Int, sim: Boolean): ControlOut = {
    require(instruct.getWidth == 32)
    val out = Wire(new ControlOut(xlen, sim))

    import InstructionMap._
    val possiblePatterns = Seq(
      LUI,
      AUIPC,
      JAL,
      JALR,
      BEQ,
      BNE,
      BLT,
      BGE,
      BLTU,
      BGEU,
      LB,
      LH,
      LW,
      LBU,
      LHU,
      SB,
      SH,
      SW,
      ADDI,
      SLTI,
      SLTIU,
      XORI,
      ORI,
      ANDI,
      SLLI,
      SRLI,
      SRAI,
      ADD,
      SUB,
      SLL,
      SLT,
      SLTU,
      XOR,
      SRL,
      SRA,
      OR,
      AND,
      FENCE,
      ECALL,
      EBREAK,
      FENCE_I,
      CSRRW,
      CSRRS,
      CSRRC,
      CSRRWI,
      CSRRSI,
      CSRRCI,
      MRET
    )
    val decodeTable = new DecodeTable(
      possiblePatterns,
      Seq(
        ImmControlField,
        needRd1ControlField,
        needRd2ControlField,
        RegWeControlField,
        WBSrcControlField,
        ALUASrcControlField,
        ALUBSrcControlField,
        ALUControlField,
        CSRSrcControlField,
        CSRControlField,
        JumpASrcControlField,
        JumpBSrcControlField,
        BrControlField,
        PCSrcControlField,
        MemRenControlField,
        MemWenControlField,
        MemOpControlField,
        FENCE_IField,
        EndControlField
      )
    )
    val decodeResult = decodeTable.decode(instruct)

    out.immType := decodeResult(ImmControlField)

    out.needRd1 := decodeResult(needRd1ControlField)
    out.needRd2 := decodeResult(needRd2ControlField)
    out.regWe   := decodeResult(RegWeControlField)
    out.wbSrc   := decodeResult(WBSrcControlField)

    val aluCtr = decodeResult(ALUControlField)
    out.aluASrc    := decodeResult(ALUASrcControlField)
    out.aluBSrc    := decodeResult(ALUBSrcControlField)
    out.aluSel     := aluCtr.aluSel
    out.isArith    := aluCtr.isArith
    out.isLeft     := aluCtr.isLeft
    out.isUnsigned := aluCtr.isUnsigned
    out.isSub      := aluCtr.isSub

    out.csrSrc := decodeResult(CSRSrcControlField)
    out.csrCtr := decodeResult(CSRControlField)

    out.jumpASrc := decodeResult(JumpASrcControlField)
    out.jumpBSrc := decodeResult(JumpBSrcControlField)
    out.brType   := decodeResult(BrControlField)
    out.pcSrc    := decodeResult(PCSrcControlField)

    out.memRen := decodeResult(MemRenControlField)
    out.memWen := decodeResult(MemWenControlField)
    out.memOp  := decodeResult(MemOpControlField)

    out.fence_i := decodeResult(FENCE_IField)
    if (sim) {
      out.isEnd.get := decodeResult(EndControlField)
    }
    out
  }
}
