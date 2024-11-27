package rvcpu.core

import rvcpu.utility._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class EXUOut(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val wa     = Output(UInt(if (extentionE) 4.W else 5.W))
  val aluOut = Output(UInt(xlen.W))
  val csrOut = Output(UInt(xlen.W))
  val wdata  = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe  = Bool()
    val wbSrc  = Output(UInt(WBSrcFrom.getWidth.W))
    val memRen = Output(Bool())
    val memWen = Output(Bool())
    val memOp  = Output(UInt(MemOp.getWidth.W))
  }

  val nextPC     = if (sim) Some(Output(UInt(xlen.W))) else None
  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
}

class EXUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in     = Flipped(DecoupledIO(new IDUOut(xlen, extentionE, sim)))
  val out    = DecoupledIO(new EXUOut(xlen, extentionE, sim))
  val jump   = Output(Bool())
  val nextPC = Output(UInt(xlen.W))
  val RegFileAccess = new Bundle {
    val wa = Output(UInt(if (extentionE) 4.W else 5.W))
    val we = Output(Bool())
  }
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

  val ALU        = Module(new ALU(xlen))
  val CSRControl = Module(new CSRControl(xlen))
  val BrCond     = Module(new BrCond)

  ALU.io.inA        := aluASrc
  ALU.io.inB        := aluBSrc
  ALU.io.aluSel     := control.aluSel
  ALU.io.isArith    := control.isArith
  ALU.io.isLeft     := control.isLeft
  ALU.io.isUnsigned := control.isUnsigned
  ALU.io.isSub      := control.isSub

  val csrSrc = MuxCase(
    DontCare,
    Seq(
      CSRSrcFrom.fromRs1 -> aluASrc,
      CSRSrcFrom.fromUimm -> uimm
    ).map { case (key, data) => (key === control.csrSrc, data) }
  )
  CSRControl.io.csrAddr := aluBSrc
  CSRControl.io.csrIn   := csrSrc
  CSRControl.io.csrCtr  := Mux(io.in.valid, control.csrCtr, CSRCtr.csrNone.value.U)
  CSRControl.io.cause   := in.exceptCause
  CSRControl.io.pc      := jumpASrc

  BrCond.io.brType := control.brType
  BrCond.io.less   := ALU.io.less
  BrCond.io.zero   := ALU.io.zero

  val jump   = io.in.valid && (BrCond.io.jump || (control.pcSrc === PCSrcFrom.fromCSR))
  val jumped = RegNext(!io.out.fire && jump)
  val npc    = jumpASrc + jumpBSrc
  val pcCom  = if (sim) Mux(!jump, in.pc.get + 4.U, npc) else npc
  val nextPC = MuxCase(
    pcCom,
    Seq(
      PCSrcFrom.fromCom -> pcCom,
      PCSrcFrom.fromCSR -> CSRControl.io.csrOut
    ).map { case (key, data) => (key === control.pcSrc, data) }
  )

  io.in.ready                := !io.in.valid || io.out.fire
  io.out.valid               := io.in.valid
  io.out.bits.wa             := in.wa
  io.out.bits.aluOut         := ALU.io.aluOut
  io.out.bits.csrOut         := CSRControl.io.csrOut
  io.out.bits.wdata          := jumpBSrc
  io.out.bits.control.regWe  := control.regWe
  io.out.bits.control.wbSrc  := control.wbSrc
  io.out.bits.control.memRen := control.memRen
  io.out.bits.control.memWen := control.memWen
  io.out.bits.control.memOp  := control.memOp
  io.RegFileAccess.wa        := in.wa
  io.RegFileAccess.we        := control.regWe && io.in.valid

  io.jump   := !jumped && jump
  io.nextPC := nextPC

  if (sim) {
    io.out.bits.nextPC.get     := nextPC
    io.out.bits.inst.get       := in.inst.get
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := in.isEnd.get
    io.out.bits.exitCode.get   := in.exitCode.get
  }
}
