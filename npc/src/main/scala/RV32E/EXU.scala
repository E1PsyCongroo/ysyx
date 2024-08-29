package RVCPU

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

class MemAccess(awidth:Int = 32, xlen:Int = 32) extends Bundle {
  val memOp   = Output(UInt(MemOp.getWidth.W))
  val ren     = Input(Bool())
  val raddr   = Input(UInt(awidth.W))
  val wen     = Input(Bool())
  val waddr   = Input(UInt(awidth.W))
  val wdata   = Input(UInt(xlen.W))
}

class MemReturn(xlen: Int = 32) extends Bundle {
  val rdata   = Output(UInt(xlen.W))
}

class EXUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val in  = Flipped(DecoupledIO(new IDUOut))
  val out = DecoupledIO(new EXUOut)
  val memAccess = DecoupledIO(new MemAccess(awidth, xlen))
  val memReturn = Flipped(DecoupledIO(new MemReturn(xlen)))
}

class EXU(xlen: Int = 32) extends Module {
  val io = IO(new EXUIO(xlen))
  val ALU         = Module(new ALU(xlen))
  val CSRControl  = Module(new CSRControl(xlen))
  val BrCond      = Module(new BrCond)

  val pc      = io.in.bits.pc
  val imm     = io.in.bits.imm
  val uimm    = io.in.bits.uimm
  val rd1     = io.in.bits.rd1
  val rd2     = io.in.bits.rd2
  val control = io.in.bits.control

  val sExec :: sFetch :: Nil = Enum(2)
  val state = RegInit(sExec)
  state := MuxLookup(state, sExec)(Seq(
    sExec     -> Mux(io.memAccess.fire && control.memRen, sFetch, sExec),
    sFetch    -> Mux(io.memReturn.fire, sExec, sFetch),
  ))

  val isExec    = state === sExec
  val isFetch   = state === sFetch

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

  val memReaded       = RegInit(false.B)
  memReaded           := Mux(io.memReturn.fire, true.B, Mux(io.in.fire, false.B, memReaded))

  io.memAccess.valid      := !reset.asBool && io.in.fire && ((control.memRen && !memReaded) || control.memWen)
  io.memAccess.bits.ren   := control.memRen
  io.memAccess.bits.memOp := control.memOp
  io.memAccess.bits.raddr := ALU.io.aluOut
  io.memAccess.bits.wen   := control.memWen
  io.memAccess.bits.waddr := ALU.io.aluOut
  io.memAccess.bits.wdata := rd2

  io.memReturn.ready      := isFetch

  io.in.ready               := isExec
  io.out.valid              := (isExec && !control.memRen && io.in.fire) || (isFetch && io.memReturn.fire)
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.pcCom         := pcASrc + pcBSrc
  io.out.bits.aluOut        := ALU.io.aluOut
  io.out.bits.memOut        := io.memReturn.bits.rdata
  io.out.bits.csrOut        := CSRControl.io.csrOut
  io.out.bits.control.regWe := io.in.bits.control.regWe
  io.out.bits.control.pcSrc := io.in.bits.control.pcSrc
  io.out.bits.control.wbSrc := io.in.bits.control.wbSrc
}

