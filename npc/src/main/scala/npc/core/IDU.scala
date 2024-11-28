package rvcpu.core

import chisel3._
import chisel3.util._

class IDUOut(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val aluASrc  = Output(UInt(xlen.W))
  val aluBSrc  = Output(UInt(xlen.W))
  val jumpASrc = Output(UInt(xlen.W))
  val jumpBSrc = Output(UInt(xlen.W))
  val ra1      = Output(UInt(if (extentionE) 4.W else 5.W))
  val wa       = Output(UInt(if (extentionE) 4.W else 5.W))
  val control = new Bundle {
    val regWe      = Output(Bool())
    val wbSrc      = Output(UInt(WBSrcFrom.getWidth.W))
    val aluSel     = Output(UInt(ALUOutSel.getWidth.W))
    val isArith    = Output(Bool())
    val isLeft     = Output(Bool())
    val isUnsigned = Output(Bool())
    val isSub      = Output(Bool())
    val csrSrc     = Output(UInt(CSRSrcFrom.getWidth.W))
    val csrCtr     = Output(UInt(CSRCtr.getWidth.W))
    val brType     = Output(UInt(BrType.getWidth.W))
    val pcSrc      = Output(UInt(PCSrcFrom.getWidth.W))
    val memRen     = Output(Bool())
    val memWen     = Output(Bool())
    val memOp      = Output(UInt(MemOp.getWidth.W))
  }

  val pc         = if (sim) Some(Output(UInt(32.W))) else None
  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
}

class IDUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in    = Flipped(DecoupledIO(new ICacheOut(xlen, sim)))
  val out   = DecoupledIO(new IDUOut(xlen, extentionE, sim))
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

  val instruction = in.instruction
  val ra1         = instruction(19, 15)
  val ra2         = instruction(24, 20)
  val wa          = instruction(11, 7)
  val pc          = in.pc
  val rd1         = io.RegFileReturn.rd1
  val rd2         = io.RegFileReturn.rd2
  val control     = Control(instruction, xlen, sim)
  val imm         = ImmGen(instruction, control.immType)

  if (sim) {
    io.RegFileAccess.ra1 := Mux(control.isEnd.get, 10.U, ra1) & Fill(5, control.needRd1)
  } else {
    io.RegFileAccess.ra1 := ra1 & Fill(5, control.needRd1)
  }
  io.RegFileAccess.ra2 := ra2 & Fill(5, control.needRd2)

  val ifenced = RegNext(!io.out.fire && control.fence_i)

  val aluASrc = MuxCase(
    DontCare,
    Seq(
      ALUASrcFrom.fromPc -> pc,
      ALUASrcFrom.fromRs1 -> rd1
    ).map { case (key, data) => (control.aluASrc === key, data) }
  )
  val aluBSrc = MuxCase(
    DontCare,
    Seq(
      ALUBSrcFrom.from4 -> 4.U,
      ALUBSrcFrom.fromRs2 -> rd2,
      ALUBSrcFrom.fromImm -> imm
    ).map { case (key, data) => (control.aluBSrc === key, data) }
  )
  val jumpASrc = MuxCase(
    DontCare,
    Seq(
      JumpASrcFrom.fromPc -> pc,
      JumpASrcFrom.fromRs1 -> rd1
    ).map { case (key, data) => (control.jumpASrc === key, data) }
  )
  val jumpBSrc = MuxCase(
    DontCare,
    Seq(
      JumpBSrcFrom.fromRs2 -> rd2,
      JumpBSrcFrom.fromImm -> imm
    ).map { case (key, data) => (control.jumpBSrc === key, data) }
  )

  io.in.ready                    := !io.in.valid || io.out.fire
  io.out.valid                   := io.in.valid && !io.stall
  io.out.bits.aluASrc            := aluASrc
  io.out.bits.aluBSrc            := aluBSrc
  io.out.bits.jumpASrc           := jumpASrc
  io.out.bits.jumpBSrc           := jumpBSrc
  io.out.bits.wa                 := wa
  io.out.bits.ra1                := ra1
  io.out.bits.control.regWe      := control.regWe
  io.out.bits.control.wbSrc      := control.wbSrc
  io.out.bits.control.aluSel     := control.aluSel
  io.out.bits.control.isArith    := control.isArith
  io.out.bits.control.isLeft     := control.isLeft
  io.out.bits.control.isUnsigned := control.isUnsigned
  io.out.bits.control.isSub      := control.isSub
  io.out.bits.control.csrSrc     := control.csrSrc
  io.out.bits.control.csrCtr     := control.csrCtr
  io.out.bits.control.brType     := control.brType
  io.out.bits.control.pcSrc      := control.pcSrc
  io.out.bits.control.memRen     := control.memRen
  io.out.bits.control.memWen     := control.memWen
  io.out.bits.control.memOp      := control.memOp
  io.fence_i                     := control.fence_i && !ifenced

  if (sim) {
    io.out.bits.pc.get         := pc
    io.out.bits.inst.get       := instruction
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := control.isEnd.get
    io.out.bits.exitCode.get   := rd1
  }
}
