package rvcpu.core

import chisel3._
import chisel3.util._

class IDUOut(xlen: Int, extentionE: Boolean) extends Bundle {
  val pc   = Output(UInt(xlen.W))
  val rd1  = Output(UInt(xlen.W))
  val rd2  = Output(UInt(xlen.W))
  val wa   = Output(UInt(if (extentionE) 4.W else 5.W))
  val imm  = Output(UInt(xlen.W))
  val uimm = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe   = Output(Bool())
    val aluASrc = Output(UInt(ALUASrcFrom.getWidth.W))
    val aluBSrc = Output(UInt(ALUBSrcFrom.getWidth.W))
    val aluCtr  = Output(UInt(ALUOp.getWidth.W))
    val csrSrc  = Output(UInt(CSRSrcFrom.getWidth.W))
    val csrCtr  = Output(UInt(CSRCtr.getWidth.W))
    val brType  = Output(UInt(BrType.getWidth.W))
    val pcSrc   = Output(UInt(PCSrcFrom.getWidth.W))
    val wbSrc   = Output(UInt(WBSrcFrom.getWidth.W))
    val memRen  = Output(Bool())
    val memWen  = Output(Bool())
    val memOp   = Output(UInt(MemOp.getWidth.W))
  }
}

class IDUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in  = Flipped(DecoupledIO(new IFUOut(xlen)))
  val out = DecoupledIO(new IDUOut(xlen, extentionE))
  val RegFileAccess = new Bundle {
    val ra1 = Output(UInt(if (extentionE) 4.W else 5.W))
    val ra2 = Output(UInt(if (extentionE) 4.W else 5.W))
  }
  val RegFileReturn = new Bundle {
    val rd1 = Input(UInt(xlen.W))
    val rd2 = Input(UInt(xlen.W))
  }
  val fence_i  = Output(Bool())
  val isEnd    = if (sim) Some(Output(Bool())) else None
  val exitCode = if (sim) Some(Output(UInt(32.W))) else None
}

class IDU(xlen: Int = 32, extentionE: Boolean = true, sim: Boolean = true) extends Module {
  val io = IO(new IDUIO(xlen, extentionE, sim))

  val in = WireDefault(io.in.bits)

  val sIdle :: sDecode :: Nil = Enum(2)

  val state    = RegInit(sIdle)
  val isIdle   = state === sIdle
  val isDecode = state === sDecode

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sDecode, sIdle),
      sDecode -> Mux(io.out.fire, sIdle, sDecode)
    )
  )

  val ImmGen  = Module(new ImmGen(xlen))
  val Control = Module(new Control(sim))

  val instruction = in.instruction
  val rs1         = instruction(19, 15)
  val rs2         = instruction(24, 20)
  val rd          = instruction(11, 7)

  if (sim) {
    io.RegFileAccess.ra1 := Mux(Control.io.isEnd.get, 10.U, rs1)
  } else {
    io.RegFileAccess.ra1 := rs1
  }
  io.RegFileAccess.ra2 := rs2

  ImmGen.io.instruction := instruction
  ImmGen.io.immType     := Control.io.immType

  Control.io.instruction := instruction

  io.in.ready                 := isIdle
  io.out.valid                := isDecode
  io.out.bits.pc              := in.pc
  io.out.bits.rd1             := io.RegFileReturn.rd1
  io.out.bits.rd2             := io.RegFileReturn.rd2
  io.out.bits.wa              := rd
  io.out.bits.imm             := ImmGen.io.imm
  io.out.bits.uimm            := rs1.pad(32)
  io.out.bits.control.regWe   := Control.io.regWe
  io.out.bits.control.aluASrc := Control.io.aluASrc
  io.out.bits.control.aluBSrc := Control.io.aluBSrc
  io.out.bits.control.aluCtr  := Control.io.aluCtr
  io.out.bits.control.csrSrc  := Control.io.csrSrc
  io.out.bits.control.csrCtr  := Control.io.csrCtr
  io.out.bits.control.brType  := Control.io.brType
  io.out.bits.control.pcSrc   := Control.io.pcSrc
  io.out.bits.control.wbSrc   := Control.io.wbSrc
  io.out.bits.control.memRen  := Control.io.memRen
  io.out.bits.control.memWen  := Control.io.memWen
  io.out.bits.control.memOp   := Control.io.memOp
  io.fence_i                  := Control.io.fence_i

  if (sim) {
    io.isEnd.get    := Control.io.isEnd.get
    io.exitCode.get := io.RegFileReturn.rd1
  }
}
