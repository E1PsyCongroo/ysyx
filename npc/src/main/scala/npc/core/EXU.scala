package rvcpu.core

import rvcpu.utility._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class EXUOut(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val wa          = Output(UInt(if (extentionE) 4.W else 5.W))
  val alu_csr_Out = Output(UInt(xlen.W))
  val wdata       = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe  = Bool()
    val wbSrc  = Output(UInt((WBSrcFrom.getWidth - 1).W))
    val memRen = Output(Bool())
    val memWen = Output(Bool())
    val memOp  = Output(UInt(MemOp.getWidth.W))
  }

  val nextPC     = if (sim) Some(Output(UInt(xlen.W))) else None
  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
  val mtvec      = if (sim) Some(Output(UInt(xlen.W))) else None
  val mepc       = if (sim) Some(Output(UInt(xlen.W))) else None
}

class EXUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in            = Flipped(DecoupledIO(new IDUOut(xlen, extentionE, sim)))
  val out           = DecoupledIO(new EXUOut(xlen, extentionE, sim))
  val jump          = Output(Bool())
  val nextPC        = Output(UInt(xlen.W))
  val RegFileAccess = Flipped(new RegFileAccess(xlen, if (extentionE) 4 else 5))
}

class EXU(xlen: Int, extentionE: Boolean, sim: Boolean) extends Module {
  val io = IO(new EXUIO(xlen, extentionE, sim))

  val in       = WireDefault(io.in.bits)
  val aluASrc  = in.aluASrc
  val aluBSrc  = in.aluBSrc
  val jumpASrc = in.jumpASrc
  val jumpBSrc = in.jumpBSrc
  val uimm     = in.ra1.pad(xlen)
  val control  = in.control

  val aluOut = ALU(control.aluSel, control.isArith, control.isLeft, control.isUnsigned, control.isSub, aluASrc, aluBSrc)
  val csrSrc = Wire(UInt(xlen.W))
  csrSrc := MuxCase(
    DontCare,
    Seq(
      CSRSrcFrom.fromRs1 -> aluASrc,
      CSRSrcFrom.fromUimm -> uimm
    ).map { case (key, data) => (key === control.csrSrc, data) }
  )
  val csrCtr = Mux(io.in.valid, control.csrCtr, CSRCtr.csrNone.value.U)
  val csr    = CSRControl(xlen, sim, csrCtr, aluBSrc(11, 0), csrSrc, jumpASrc)

  val brJump = BrCond(control.brType, aluOut.less, aluOut.zero)
  val jump   = io.in.valid && (brJump || (control.pcSrc === PCSrcFrom.fromCSR))
  val jumped = RegNext(!io.out.fire && jump)
  val npc    = jumpASrc + jumpBSrc
  val pcCom  = if (sim) Mux(!jump, in.pc.get + 4.U, npc) else npc
  val nextPC = MuxCase(
    pcCom,
    Seq(
      PCSrcFrom.fromCom -> pcCom,
      PCSrcFrom.fromCSR -> csr.out
    ).map { case (key, data) => (key === control.pcSrc, data) }
  )

  io.in.ready                := !io.in.valid || io.out.fire
  io.out.valid               := io.in.valid
  io.out.bits.wa             := in.wa
  io.out.bits.alu_csr_Out    := Mux(control.wbSrc(1), csr.out, aluOut.out)
  io.out.bits.wdata          := jumpBSrc
  io.out.bits.control.regWe  := control.regWe(0)
  io.out.bits.control.wbSrc  := control.wbSrc
  io.out.bits.control.memRen := control.memRen
  io.out.bits.control.memWen := control.memWen
  io.out.bits.control.memOp  := control.memOp
  io.RegFileAccess.ra1       := DontCare
  io.RegFileAccess.ra2       := DontCare
  io.RegFileAccess.wa        := in.wa
  io.RegFileAccess.we        := control.regWe && io.in.valid
  io.RegFileAccess.wd        := DontCare

  io.jump   := !jumped && jump
  io.nextPC := nextPC

  if (sim) {
    io.out.bits.nextPC.get     := nextPC
    io.out.bits.inst.get       := in.inst.get
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := in.isEnd.get
    io.out.bits.exitCode.get   := in.exitCode.get
    io.out.bits.mtvec.get      := csr.mtvec.get
    io.out.bits.mepc.get       := csr.mepc.get
  }
}
