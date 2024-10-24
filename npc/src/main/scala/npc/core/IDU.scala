package rvcpu.core

import chisel3._
import chisel3.util._

class IDUOut(xlen: Int = 32) extends Bundle {
  val pc   = Output(UInt(xlen.W))
  val rd1  = Output(UInt(xlen.W))
  val rd2  = Output(UInt(xlen.W))
  val wa   = Output(UInt(5.W))
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

class IDUIO(xlen: Int = 32, extentionE: Boolean = true) extends Bundle {
  val in  = Flipped(DecoupledIO(new IFUOut))
  val out = DecoupledIO(new IDUOut)
  val RegFileAccess = new Bundle {
    val ra1 = Output(UInt(if (extentionE) 4.W else 5.W))
    val ra2 = Output(UInt(if (extentionE) 4.W else 5.W))
  }
  val RegFileReturn = new Bundle {
    val rd1 = Input(UInt(xlen.W))
    val rd2 = Input(UInt(xlen.W))
  }
}

class EndControlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val isEnd = Input(Bool())
  val code  = Input(UInt(32.W))
}
class EndControl extends BlackBox with HasBlackBoxResource {
  val io = IO(new EndControlIO)
  addResource("/EndControl.sv")
}

class IDU(xlen: Int = 32, extentionE: Boolean = true) extends Module {
  val io = IO(new IDUIO(xlen, extentionE))

  val sExec :: Nil = Enum(1)
  val state        = RegInit(sExec)
  state := MuxLookup(state, sExec)(
    Seq(
      sExec -> sExec
    )
  )

  val isExec = state === sExec

  val ImmGen     = Module(new ImmGen(xlen))
  val Control    = Module(new Control)
  val EndControl = Module(new EndControl)

  val instruction = io.in.bits.instruction
  val rs1         = instruction(19, 15)
  val rs2         = instruction(24, 20)
  val rd          = instruction(11, 7)

  io.RegFileAccess.ra1 := Mux(Control.io.isEnd, 10.U(5.W), rs1)
  io.RegFileAccess.ra2 := rs2

  ImmGen.io.instruction := instruction
  ImmGen.io.immType     := Control.io.immType

  Control.io.instruction := instruction

  EndControl.io.clock := clock
  EndControl.io.reset := reset
  EndControl.io.isEnd := Control.io.isEnd
  EndControl.io.code  := io.RegFileReturn.rd1

  io.in.ready                 := io.out.ready
  io.out.valid                := io.in.valid
  io.out.bits.pc              := io.in.bits.pc
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
}
