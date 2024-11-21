package rvcpu.core

import rvcpu.utility._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class EXUOut(xlen: Int, extentionE: Boolean) extends Bundle {
  val wa     = Output(UInt(if (extentionE) 4.W else 5.W))
  val pcCom  = Output(UInt(xlen.W))
  val aluOut = Output(UInt(xlen.W))
  val csrOut = Output(UInt(xlen.W))
  val wdata  = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe  = Bool()
    val pcSrc  = Output(UInt(PCSrcFrom.getWidth.W))
    val wbSrc  = Output(UInt(WBSrcFrom.getWidth.W))
    val memRen = Output(Bool())
    val memWen = Output(Bool())
    val memOp  = Output(UInt(MemOp.getWidth.W))
  }
}

class EXUIO(xlen: Int, extentionE: Boolean) extends Bundle {
  val in  = Flipped(DecoupledIO(new IDUOut(xlen, extentionE)))
  val out = DecoupledIO(new EXUOut(xlen, extentionE))
}

class EXU(xlen: Int, extentionE: Boolean) extends Module {
  val io = IO(new EXUIO(xlen, extentionE))

  val in      = WireDefault(io.in.bits)
  val pc      = in.pc
  val imm     = in.imm
  val uimm    = in.uimm
  val rd1     = in.rd1
  val rd2     = in.rd2
  val control = in.control

  val ALU        = Module(new ALU(xlen))
  val CSRControl = Module(new CSRControl(xlen))
  val BrCond     = Module(new BrCond)

  val sIdle :: sExec :: Nil = Enum(2)

  val state  = RegInit(sIdle)
  val isIdle = state === sIdle
  val isExec = state === sExec

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sExec, sIdle),
      sExec -> Mux(io.out.fire, sIdle, sExec)
    )
  )

  val aluASrc = MuxCase(
    pc,
    Seq(
      ALUASrcFrom.fromPc -> pc,
      ALUASrcFrom.fromRs1 -> rd1
    ).map { case (key, data) => (control.aluASrc === key, data) }
  )
  val aluBSrc = MuxCase(
    4.U,
    Seq(
      ALUBSrcFrom.from4 -> 4.U,
      ALUBSrcFrom.fromRs2 -> rd2,
      ALUBSrcFrom.fromImm -> imm
    ).map { case (key, data) => (control.aluBSrc === key, data) }
  )
  ALU.io.inA    := aluASrc
  ALU.io.inB    := aluBSrc
  ALU.io.aluCtr := control.aluCtr

  val csrSrc = MuxCase(
    rd1,
    Seq(
      CSRSrcFrom.fromRs1 -> rd1,
      CSRSrcFrom.fromUimm -> uimm
    ).map { case (key, data) => (key === control.csrSrc, data) }
  )
  CSRControl.io.csrAddr := imm
  CSRControl.io.epc     := pc
  CSRControl.io.csrIn   := csrSrc
  CSRControl.io.csrCtr  := Mux(isExec, control.csrCtr, CSRCtr.csrNone.value.U)

  BrCond.io.brType := control.brType
  BrCond.io.less   := ALU.io.less
  BrCond.io.zero   := ALU.io.zero
  val pcASrc = Mux(BrCond.io.PCASrc, imm, 4.U)
  val pcBSrc = Mux(BrCond.io.PCBSrc, pc, rd1)
  val pcCom  = pcASrc + pcBSrc

  io.in.ready                := isIdle
  io.out.valid               := isExec
  io.out.bits.wa             := in.wa
  io.out.bits.pcCom          := pcCom
  io.out.bits.aluOut         := ALU.io.aluOut
  io.out.bits.csrOut         := CSRControl.io.csrOut
  io.out.bits.wdata          := rd2
  io.out.bits.control.regWe  := control.regWe
  io.out.bits.control.pcSrc  := control.pcSrc
  io.out.bits.control.wbSrc  := control.wbSrc
  io.out.bits.control.memRen := control.memRen
  io.out.bits.control.memWen := control.memWen
  io.out.bits.control.memOp  := control.memOp
}
