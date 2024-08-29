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

class EXUIO(xlen: Int = 32) extends Bundle {
  val in  = Flipped(DecoupledIO(new IDUOut))
  val out = DecoupledIO(new EXUOut)
}

class EXU(xlen: Int = 32) extends Module {
  val io = IO(new EXUIO(xlen))

  val ALU         = Module(new ALU(xlen))
  val CSRControl  = Module(new CSRControl(xlen))
  val BrCond      = Module(new BrCond)
  val LSU         = Module(new LSU)
  val LSUSetAddrFire  = LSU.io.avalid && LSU.io.aready
  val LSUFetchFire    = LSU.io.dvalid && LSU.io.dready

  val pc      = io.in.bits.pc
  val imm     = io.in.bits.imm
  val uimm    = io.in.bits.uimm
  val rd1     = io.in.bits.rd1
  val rd2     = io.in.bits.rd2
  val control = io.in.bits.control

  val sExec :: sFetch :: Nil = Enum(2)
  val state = RegInit(sExec)
  state := MuxLookup(state, sExec)(Seq(
    sExec     -> Mux(LSUSetAddrFire && control.memRen, sFetch, sExec),
    sFetch    -> Mux(LSUFetchFire, sExec, sFetch),
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
  memReaded           := Mux(LSUFetchFire, true.B, Mux(io.in.fire, false.B, memReaded))

  LSU.io.avalid       := !reset.asBool && io.in.fire && (control.memRen || control.memWen) && !memReaded
  LSU.io.memOp        := control.memOp
  LSU.io.raddr        := ALU.io.aluOut
  LSU.io.wen          := control.memWen
  LSU.io.waddr        := ALU.io.aluOut
  LSU.io.wdata        := rd2
  LSU.io.dready       := isFetch
  val memOut          = RegEnable(LSU.io.rdata, LSUFetchFire)

  io.in.ready               := isExec
  io.out.valid              := (isExec && !control.memRen && io.in.fire) || (isExec && memReaded)
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.pcCom         := pcASrc + pcBSrc
  io.out.bits.aluOut        := ALU.io.aluOut
  io.out.bits.memOut        := memOut
  io.out.bits.csrOut        := CSRControl.io.csrOut
  io.out.bits.control.regWe := io.in.bits.control.regWe
  io.out.bits.control.pcSrc := io.in.bits.control.pcSrc
  io.out.bits.control.wbSrc := io.in.bits.control.wbSrc
}

