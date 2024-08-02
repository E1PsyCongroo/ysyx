package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import org.apache.commons.compress.harmony.unpack200.bytecode.forms.ReferenceForm
import coursier.core.shaded.geny.Generator.End

class EndControlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val isEnd = Input(Bool())
}
class EndControl extends BlackBox with HasBlackBoxResource {
  val io = IO(new EndControlIO)
  addResource("/EndControl.sv")
}

class RVCPU(
  xlen: Int = 32,
  extentionE: Boolean = true,
  extentionC: Boolean = false,
  PCReset: BigInt = BigInt("0x8000_0000")
) extends Module {
  val io = IO(new Bundle{
    val inst = Input(UInt(if (extentionC) 16.W else 32.W))
    val pc = Output(UInt(xlen.W))
  })
  val PCnext      = Wire(UInt(xlen.W))
  val PC          = RegNext(PCnext, PCReset.U(xlen.W))
  val Mem         = Module(new MemControl)
  val RegFile     = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val ImmGen      = Module(new ImmGen(xlen))
  val Control     = Module(new Control)
  val ALU         = Module(new ALU(xlen))
  val BrCond      = Module(new BrCond)
  val EndControl  = Module(new EndControl)

  /* Instruction Fetch */
  io.pc       := PCnext

  /* Instruction Decode */
  val inst    = io.inst
  val rs1     = inst(19, 15)
  val rs2     = inst(24, 20)
  val rd      = inst(11, 7)

  RegFile.io.ra1      := rs1
  RegFile.io.ra2      := rs2
  RegFile.io.wa       := rd

  ImmGen.io.instr     := inst
  ImmGen.io.immType   := Control.io.immType

  Control.io.instr    := inst

  EndControl.io.clock := clock
  EndControl.io.reset := reset
  EndControl.io.isEnd := Control.io.isEnd

  /* Instruction Execution */
  val aluASrc = MuxLookup(Control.io.aluASrc, PC)(Seq(
    ALUASrcFrom.fromPc.asUInt   -> PC,
    ALUASrcFrom.fromRs1.asUInt  -> RegFile.io.rd1,
  ))
  val aluBSrc = MuxLookup(Control.io.aluBSrc, 4.U)(Seq(
    ALUBSrcFrom.from4.asUInt    -> 4.U,
    ALUBSrcFrom.fromRs2.asUInt  -> RegFile.io.rd2,
    ALUBSrcFrom.fromImm.asUInt  -> ImmGen.io.imm,
  ))
  ALU.io.inA          := aluASrc
  ALU.io.inB          := aluBSrc
  ALU.io.aluCtr       := Control.io.aluCtr

  BrCond.io.brType    := Control.io.brType
  BrCond.io.less      := ALU.io.less
  BrCond.io.zero      := ALU.io.zero
  val PCASrc = Mux(BrCond.io.PCASrc, ImmGen.io.imm, 4.U)
  val PCBSrc = Mux(BrCond.io.PCBSrc, PC, RegFile.io.rd1)
  PCnext              := PCASrc + PCBSrc

  /* Memory */
  Mem.io.valid        := true.B
  Mem.io.memOp        := Control.io.memOp
  Mem.io.raddr        := ALU.io.aluOut

  Mem.io.wen          := Control.io.memWe
  Mem.io.waddr        := ALU.io.aluOut
  Mem.io.wdata        := RegFile.io.rd2

  /* Write Back */
  val writeToReg = MuxLookup(Control.io.wbSrc, ALU.io.aluOut)(Seq(
    WBSrcFrom.fromALU.asUInt -> ALU.io.aluOut,
    WBSrcFrom.fromMem.asUInt -> Mem.io.raddr,
  ))
  RegFile.io.wd       := writeToReg
}
