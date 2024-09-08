package rvcpu.core

import rvcpu.utility._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class EXUOut(xlen: Int = 32) extends Bundle {
  val wa      = Output(UInt(5.W))
  val pcCom   = Output(UInt(xlen.W))
  val aluOut  = Output(UInt(xlen.W))
  val memOut  = Output(UInt(xlen.W))
  val csrOut  = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe = Bool()
    val pcSrc = Output(UInt(PCSrcFrom.getWidth.W))
    val wbSrc = Output(UInt(WBSrcFrom.getWidth.W))
  }
}

class LSUIn(xlen: Int = 32) extends Bundle {
  val memOp     = Output(UInt(MemOp.getWidth.W))
  val ren       = Output(Bool())
  val raddr     = Output(UInt(xlen.W))
  val wen       = Output(Bool())
  val waddr     = Output(UInt(xlen.W))
  val wdata     = Output(UInt(xlen.W))
}

class EXUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val in          = Flipped(DecoupledIO(new IDUOut))
  val out         = DecoupledIO(new EXUOut)
  val LSUIn       = DecoupledIO(new LSUIn)
  val LSUOut      = Flipped(DecoupledIO(new LSUOut))
}

class EXU(xlen: Int = 32) extends Module {
  val io          = IO(new EXUIO(xlen))
  val ALU         = Module(new ALU(xlen))
  val CSRControl  = Module(new CSRControl(xlen))
  val BrCond      = Module(new BrCond)

  val pc      = io.in.bits.pc
  val imm     = io.in.bits.imm
  val uimm    = io.in.bits.uimm
  val rd1     = io.in.bits.rd1
  val rd2     = io.in.bits.rd2
  val control = io.in.bits.control

  val sExec :: sRead :: Nil = Enum(2)
  val state = RegInit(sExec)
  state := MuxLookup(state, sExec)(Seq(
    sExec    -> Mux(io.LSUIn.fire && io.LSUIn.bits.ren, sRead, sExec),
    sRead    -> Mux(io.out.fire, sExec, sRead),
  ))

  val isExec    = state === sExec
  val isRead    = state === sRead

  val aluASrc = MuxCase(pc, Seq(
    ALUASrcFrom.fromPc   -> pc,
    ALUASrcFrom.fromRs1  -> rd1,
  ).map{ case(key, data) => (control.aluASrc === key, data) })
  val aluBSrc = MuxCase(4.U, Seq(
    ALUBSrcFrom.from4   -> 4.U,
    ALUBSrcFrom.fromRs2 -> rd2,
    ALUBSrcFrom.fromImm -> imm,
  ).map{ case(key, data) => (control.aluBSrc === key, data) })
  ALU.io.inA          := aluASrc
  ALU.io.inB          := aluBSrc
  ALU.io.aluCtr       := control.aluCtr

  val csrSrc  = MuxCase(rd1, Seq(
    CSRSrcFrom.fromRs1  -> rd1,
    CSRSrcFrom.fromUimm -> uimm
  ).map{ case(key, data) => (key === control.csrSrc, data) })
  CSRControl.io.csrAddr := imm
  CSRControl.io.epc     := pc
  CSRControl.io.csrIn   := csrSrc
  CSRControl.io.csrCtr  := Mux(isExec && io.in.fire, control.csrCtr, CSRCtr.csrNone.value.U)

  BrCond.io.brType    := control.brType
  BrCond.io.less      := ALU.io.less
  BrCond.io.zero      := ALU.io.zero
  val pcASrc          = Mux(BrCond.io.PCASrc, imm, 4.U)
  val pcBSrc          = Mux(BrCond.io.PCBSrc, pc, rd1)

  /* IO bind */
  io.LSUIn.valid            := isExec && io.in.fire
  io.LSUIn.bits.memOp       := control.memOp
  io.LSUIn.bits.ren         := control.memRen
  io.LSUIn.bits.raddr       := ALU.io.aluOut
  io.LSUIn.bits.wen         := control.memWen
  io.LSUIn.bits.waddr       := ALU.io.aluOut
  io.LSUIn.bits.wdata       := rd2

  io.LSUOut.ready           := isRead

  io.in.ready               := isExec
  io.out.valid              := (isExec && io.in.fire && !control.memRen && !control.memWen) ||
                               (isRead && io.LSUOut.fire)
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.pcCom         := pcASrc + pcBSrc
  io.out.bits.aluOut        := ALU.io.aluOut
  io.out.bits.memOut        := io.LSUOut.bits.rdata
  io.out.bits.csrOut        := CSRControl.io.csrOut
  io.out.bits.control.regWe := io.in.bits.control.regWe
  io.out.bits.control.pcSrc := io.in.bits.control.pcSrc
  io.out.bits.control.wbSrc := io.in.bits.control.wbSrc
}

