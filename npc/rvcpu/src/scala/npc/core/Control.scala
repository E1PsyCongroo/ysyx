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

object ALUControlField extends DecodeField[Instruction, ALUControl] {
  def name = "ALU Control Field"
  def chiselType: ALUControl = new ALUControl
  def genTable(op: Instruction): BitPat = {
    import InstructionMap._
    import InstructionType._
    import ALUOutSel._
    val aluSel = op match {
      case ADD | ADDI | SUB | AUIPC             => selectAdder
      case JAL | JALR                           => selectAdder
      case LB | LH | LW | LBU | LHU             => selectAdder
      case SH | SB | SW                         => selectAdder
      case SLL | SLLI | SRL | SRLI | SRA | SRAI => selectShift
      case SLT | SLTI | SLTU | SLTIU            => selectSlt
      case LUI                                  => selectB
      case XOR | XORI                           => selectXor
      case OR | ORI                             => selectOr
      case AND | ANDI                           => selectAnd
      case MUL                                  => selectMul
      case MULH                                 => selectMulh1
      case MULHSU | MULHU                       => selectMulh2
      case DIV | DIVU                           => selectDiv
      case REM | REMU                           => selectRem
      case _                                    => BitPat.N(1) ## BitPat.dontCare(ALUOutSel.getWidth - 1)
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
      case SLT | SLTI | BLT | BGE     => BitPat.N(2)
      case SLTU | SLTIU | BLTU | BGEU => BitPat.Y(2)
      case MULH | DIV | REM           => BitPat.N(2)
      case MULHSU                     => BitPat("b01")
      case MULHU | DIVU | REMU        => BitPat.Y(2)
      case _                          => BitPat.dontCare(2)
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

class ControlOut(cpuConfig: CPUConfig) extends Bundle {
  val immType = UInt(ImmType.getWidth.W)

  val needRd1 = Bool()
  val needRd2 = Bool()
  val regWe   = Bool()
  val wbSrc   = UInt(WBSrcFrom.getWidth.W)

  val aluASrc    = UInt(ALUASrcFrom.getWidth.W)
  val aluBSrc    = UInt(ALUBSrcFrom.getWidth.W)
  val aluControl = new ALUControl

  val csrSrc = UInt(CSRSrcFrom.getWidth.W)
  val csrCtr = UInt(CSRCtr.getWidth.W)

  val jumpASrc = UInt(JumpASrcFrom.getWidth.W)
  val jumpBSrc = UInt(JumpBSrcFrom.getWidth.W)
  val brType   = UInt(BrType.getWidth.W)
  val pcSrc    = UInt(PCSrcFrom.getWidth.W)

  val memRen = Bool()
  val memWen = Bool()
  val memOp  = UInt(MemOp.getWidth.W)

  val fence_i = Bool()

  val isEnd = if (cpuConfig.sim) Some(Bool()) else None
}

class Control(cpuConfig: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))
    val control     = new ControlOut(cpuConfig)
  })

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
    MUL,
    MULH,
    MULHSU,
    MULHU,
    DIV,
    DIVU,
    REM,
    REMU,
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
  val decodeResult = decodeTable.decode(io.instruction)

  io.control.immType := decodeResult(ImmControlField)

  io.control.needRd1 := decodeResult(needRd1ControlField)
  io.control.needRd2 := decodeResult(needRd2ControlField)
  io.control.regWe   := decodeResult(RegWeControlField)
  io.control.wbSrc   := decodeResult(WBSrcControlField)

  io.control.aluASrc    := decodeResult(ALUASrcControlField)
  io.control.aluBSrc    := decodeResult(ALUBSrcControlField)
  io.control.aluControl := decodeResult(ALUControlField)

  io.control.csrSrc := decodeResult(CSRSrcControlField)
  io.control.csrCtr := decodeResult(CSRControlField)

  io.control.jumpASrc := decodeResult(JumpASrcControlField)
  io.control.jumpBSrc := decodeResult(JumpBSrcControlField)
  io.control.brType   := decodeResult(BrControlField)
  io.control.pcSrc    := decodeResult(PCSrcControlField)

  io.control.memRen := decodeResult(MemRenControlField)
  io.control.memWen := decodeResult(MemWenControlField)
  io.control.memOp  := decodeResult(MemOpControlField)

  io.control.fence_i := decodeResult(FENCE_IField)

  if (cpuConfig.sim) {
    io.control.isEnd.get := decodeResult(EndControlField)
  }
}
