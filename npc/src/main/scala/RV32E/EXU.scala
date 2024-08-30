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

class EXUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val in          = Flipped(DecoupledIO(new IDUOut))
  val out         = DecoupledIO(new EXUOut)
  val AXIManager  = Flipped(new AXILiteSubordinateIO(awidth, xlen))
  val memOp       = Output(UInt(MemOp.getWidth.W))
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

  val awfire  = io.AXIManager.awvalid && io.AXIManager.awready
  val wfire   = io.AXIManager.wvalid && io.AXIManager.wready
  val bfire   = io.AXIManager.bvalid && io.AXIManager.bready
  val arfire  = io.AXIManager.arvalid && io.AXIManager.arready
  val rfire   = io.AXIManager.rvalid && io.AXIManager.rready

  val sExec :: sWrite :: sRead :: Nil = Enum(3)
  val state = RegInit(sExec)
  state := MuxLookup(state, sExec)(Seq(
    sExec     -> MuxCase(sExec, Seq(
      (awfire && wfire) -> sWrite,
      arfire            -> sRead,
    )),
    sWrite   -> Mux(bfire, sExec, sWrite),
    sRead    -> Mux(rfire, sExec, sRead),
  ))

  val isExec    = state === sExec
  val isWrite   = state === sWrite
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

  val memReaded       = RegInit(false.B)
  memReaded           := Mux(rfire, true.B, Mux(io.in.fire, false.B, memReaded))

  /* IO bind */
  /* Write address channel */
  io.AXIManager.awvalid := io.in.fire && control.memWen
  io.AXIManager.awid    := DontCare
  io.AXIManager.awaddr  := ALU.io.aluOut
  io.AXIManager.awport  := DontCare

  /* Write data channel */
  io.AXIManager.wvalid  := io.in.fire && control.memWen
  io.AXIManager.wdata   := rd2
  io.AXIManager.wstarb  := DontCare

  /* Write response channel */
  io.AXIManager.bready  := isWrite

  /* Read address channel */
  io.AXIManager.arvalid := io.in.fire && control.memRen
  io.AXIManager.arid    := DontCare
  io.AXIManager.araddr  := ALU.io.aluOut
  io.AXIManager.arport  := DontCare

  /* Read data channel */
  io.AXIManager.rready  := isRead

  /* With AXI read address channel && write address channel */
  io.memOp          := control.memOp

  io.in.ready               := isExec
  io.out.valid              := (isExec && io.in.fire && !control.memRen && !control.memWen) ||
                               (isRead && rfire) ||  (isWrite && bfire)
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.pcCom         := pcASrc + pcBSrc
  io.out.bits.aluOut        := ALU.io.aluOut
  io.out.bits.memOut        := io.AXIManager.rdata
  io.out.bits.csrOut        := CSRControl.io.csrOut
  io.out.bits.control.regWe := io.in.bits.control.regWe
  io.out.bits.control.pcSrc := io.in.bits.control.pcSrc
  io.out.bits.control.wbSrc := io.in.bits.control.wbSrc
}

