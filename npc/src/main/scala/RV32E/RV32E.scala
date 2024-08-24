package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class EndControlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val isEnd = Input(Bool())
}
class EndControl extends BlackBox with HasBlackBoxResource{
  val io = IO(new EndControlIO)
  addResource("/EndControl.sv")
}

class IfetchIO(extentionC: Boolean = false) extends Bundle {
  val inst = Input(UInt(if (extentionC) 16.W else 32.W))
}

class Ifetch(extentionC: Boolean = false) extends BlackBox with HasBlackBoxResource {
  val io = IO(new IfetchIO(extentionC))
  addResource("/Ifetch.sv")
}

class RVCPU(
  xlen: Int = 32,
  extentionE: Boolean = true,
  extentionC: Boolean = false,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val io = IO(new Bundle{
    val inst = Input(UInt(if (extentionC) 16.W else 32.W))
    val pc = Output(UInt(xlen.W))
  })
  val NextPC      = Wire(UInt(xlen.W))
  val PC          = RegNext(NextPC)
  val Mem         = Module(new MemControl)
  val RegFile     = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val ImmGen      = Module(new ImmGen(xlen))
  val Control     = Module(new Control)
  val ALU         = Module(new ALU(xlen))
  val BrCond      = Module(new BrCond)
  val EndControl  = Module(new EndControl)
  val Ifetch      = Module(new Ifetch(extentionC))

  /* Instruction Fetch */
  io.pc           := NextPC

  val inst        = io.inst
  Ifetch.io.inst  := inst

  /* Instruction Decode */
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
  val aluASrc = MuxCase(PC, Seq(
    ALUASrcFrom.fromPc   -> PC,
    ALUASrcFrom.fromRs1  -> RegFile.io.rd1,
  ).map{ case(key, data) => (Control.io.aluASrc === key, data) })
  val aluBSrc = MuxCase(4.U, Seq(
    ALUBSrcFrom.from4   -> 4.U,
    ALUBSrcFrom.fromRs2 -> RegFile.io.rd2,
    ALUBSrcFrom.fromImm -> ImmGen.io.imm,
  ).map { case(key, data) => (Control.io.aluBSrc === key, data) })
  ALU.io.inA          := aluASrc
  ALU.io.inB          := aluBSrc
  ALU.io.aluCtr       := Control.io.aluCtr

  BrCond.io.brType    := Control.io.brType
  BrCond.io.less      := ALU.io.less
  BrCond.io.zero      := ALU.io.zero
  val PCASrc          = Mux(BrCond.io.PCASrc, ImmGen.io.imm, 4.U)
  val PCBSrc          = Mux(BrCond.io.PCBSrc, PC, RegFile.io.rd1)
  NextPC              := Mux(reset.asBool, PCReset.U, PCASrc + PCBSrc)

  /* Memory */
  Mem.io.valid        := !reset.asBool && Control.io.memValid
  Mem.io.memOp        := Control.io.memOp
  Mem.io.raddr        := ALU.io.aluOut

  Mem.io.wen          := Control.io.memWe
  Mem.io.waddr        := ALU.io.aluOut
  Mem.io.wdata        := RegFile.io.rd2

  /* Write Back */
  val writeToReg = MuxCase(ALU.io.aluOut, Seq(
    WBSrcFrom.fromALU -> ALU.io.aluOut,
    WBSrcFrom.fromMem -> Mem.io.rdata,
  ).map{ case(key, data) => (Control.io.wbSrc === key, data) })
  RegFile.io.we       := Control.io.regWe
  RegFile.io.wd       := writeToReg
}
