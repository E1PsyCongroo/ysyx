package rvcpu.core

import chisel3._
import chisel3.util._

class IDUOut(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val ra1         = Output(UInt(if (extentionE) 4.W else 5.W))
  val rd1         = Output(UInt(xlen.W))
  val rd2         = Output(UInt(xlen.W))
  val wa          = Output(UInt(if (extentionE) 4.W else 5.W))
  val imm         = Output(UInt(xlen.W))
  val exceptCause = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe   = Output(Bool())
    val wbSrc   = Output(UInt(WBSrcFrom.getWidth.W))
    val aluASrc = Output(UInt(ALUASrcFrom.getWidth.W))
    val aluBSrc = Output(UInt(ALUBSrcFrom.getWidth.W))
    val aluCtr  = Output(UInt(ALUOp.getWidth.W))
    val csrSrc  = Output(UInt(CSRSrcFrom.getWidth.W))
    val csrCtr  = Output(UInt(CSRCtr.getWidth.W))
    val brType  = Output(UInt(BrType.getWidth.W))
    val pcSrc   = Output(UInt(PCSrcFrom.getWidth.W))
    val memRen  = Output(Bool())
    val memWen  = Output(Bool())
    val memOp   = Output(UInt(MemOp.getWidth.W))
  }

  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
}

class IDUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in    = Flipped(DecoupledIO(new ICacheOut(xlen, sim)))
  val out   = DecoupledIO(new IDUOut(xlen, extentionE, sim))
  val flush = Input(Bool())
  val stall = Input(Bool())
  val RegFileAccess = new Bundle {
    val ra1 = Output(UInt(if (extentionE) 4.W else 5.W))
    val ra2 = Output(UInt(if (extentionE) 4.W else 5.W))
  }
  val RegFileReturn = new Bundle {
    val rd1 = Input(UInt(xlen.W))
    val rd2 = Input(UInt(xlen.W))
  }
  val fence_i = Output(Bool())
}

class IDU(xlen: Int = 32, extentionE: Boolean = true, sim: Boolean = true) extends Module {
  val io = IO(new IDUIO(xlen, extentionE, sim))

  val in = WireDefault(io.in.bits)

  val ImmGen  = Module(new ImmGen(xlen))
  val Control = Module(new Control(xlen, sim))

  val instruction = in.instruction
  val ra1         = instruction(19, 15)
  val ra2         = instruction(24, 20)
  val wa          = instruction(11, 7)
  val pc          = in.pc
  val imm         = ImmGen.io.imm
  val rd1         = io.RegFileReturn.rd1
  val rd2         = io.RegFileReturn.rd2

  if (sim) {
    io.RegFileAccess.ra1 := Mux(Control.io.isEnd.get, 10.U, ra1) & Fill(5, Control.io.needRd1)
  } else {
    io.RegFileAccess.ra1 := ra1 & Fill(5, Control.io.needRd1)
  }
  io.RegFileAccess.ra2 := ra2 & Fill(5, Control.io.needRd2)

  ImmGen.io.instruction := instruction
  ImmGen.io.immType     := Control.io.immType

  Control.io.instruction := instruction

  val ifenced = RegNext(!io.out.fire && Control.io.fence_i)

  io.in.ready                 := !io.in.valid
  io.out.valid                := io.in.valid && !io.flush && !io.stall
  io.out.bits.pc              := pc
  io.out.bits.rd1             := rd1
  io.out.bits.rd2             := rd2
  io.out.bits.wa              := wa
  io.out.bits.imm             := imm
  io.out.bits.ra1             := ra1
  io.out.bits.exceptCause     := Control.io.exceptCause
  io.out.bits.control.regWe   := Control.io.regWe
  io.out.bits.control.wbSrc   := Control.io.wbSrc
  io.out.bits.control.aluASrc := Control.io.aluASrc
  io.out.bits.control.aluBSrc := Control.io.aluBSrc
  io.out.bits.control.aluCtr  := Control.io.aluCtr
  io.out.bits.control.csrSrc  := Control.io.csrSrc
  io.out.bits.control.csrCtr  := Control.io.csrCtr
  io.out.bits.control.brType  := Control.io.brType
  io.out.bits.control.pcSrc   := Control.io.pcSrc
  io.out.bits.control.memRen  := Control.io.memRen
  io.out.bits.control.memWen  := Control.io.memWen
  io.out.bits.control.memOp   := Control.io.memOp
  io.fence_i                  := Control.io.fence_i && !ifenced

  if (sim) {
    io.out.bits.inst.get       := instruction
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := Control.io.isEnd.get
    io.out.bits.exitCode.get   := io.RegFileReturn.rd1
  }
}
