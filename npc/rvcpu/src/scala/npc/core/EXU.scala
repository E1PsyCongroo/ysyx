package rvcpu.core

import rvcpu.utility._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class EXUOut(cpuConfig: CPUConfig) extends Bundle {
  val wa          = UInt(if (cpuConfig.extentionE) 4.W else 5.W)
  val alu_csr_out = UInt(cpuConfig.xlen.W)
  val wdata       = UInt(cpuConfig.xlen.W)
  val control = new Bundle {
    val regWe  = Bool()
    val wbSrc  = UInt((WBSrcFrom.getWidth - 1).W)
    val memRen = Bool()
    val memWen = Bool()
    val memOp  = UInt(MemOp.getWidth.W)
  }

  val state = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class EXUIO(cpuConfig: CPUConfig) extends Bundle {
  val in            = Flipped(DecoupledIO(new IDUOut(cpuConfig)))
  val out           = DecoupledIO(new EXUOut(cpuConfig))
  val jump          = Output(Bool())
  val nextPC        = Output(UInt(cpuConfig.xlen.W))
  val RegFileAccess = Flipped(new RegFileAccess(cpuConfig))

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
}

class EXU(cpuConfig: CPUConfig) extends Module {
  val io  = IO(new EXUIO(cpuConfig))
  val alu = Module(new ALU(cpuConfig))
  val br  = Module(new BrCond(cpuConfig))
  val csr = Module(new CSRControl(cpuConfig))

  val in       = WireDefault(io.in.bits)
  val aluASrc  = in.aluASrc
  val aluBSrc  = in.aluBSrc
  val jumpASrc = in.jumpASrc
  val jumpBSrc = in.jumpBSrc
  val uimm     = in.ra1.pad(cpuConfig.xlen)
  val control  = in.control

  alu.io.flush           := !io.in.valid
  alu.io.in.valid        := io.in.valid
  alu.io.in.bits.control := control.aluControl
  alu.io.in.bits.inA     := aluASrc
  alu.io.in.bits.inB     := aluBSrc
  alu.io.out.ready       := io.out.ready

  br.io.brType := control.brType
  br.io.less   := alu.io.out.bits.less
  br.io.zero   := alu.io.out.bits.zero
  val jump   = io.in.valid && (br.io.jump || (control.pcSrc === PCSrcFrom.fromCSR))
  val jumped = RegNext(!io.out.fire && jump)
  val npc    = jumpASrc + jumpBSrc
  val pcCom  = if (cpuConfig.sim) Mux(!jump, in.state.get.pc + 4.U, npc) else npc
  val nextPC = MuxCase(
    pcCom,
    Seq(
      PCSrcFrom.fromCom -> pcCom,
      PCSrcFrom.fromCSR -> csr.io.out.csrout
    ).map { case (key, data) => (key === control.pcSrc, data) }
  )

  val csrSrc = Wire(UInt(cpuConfig.xlen.W))
  csrSrc := MuxCase(
    DontCare,
    Seq(
      CSRSrcFrom.fromRs1 -> aluASrc,
      CSRSrcFrom.fromUimm -> uimm
    ).map { case (key, data) => (key === control.csrSrc, data) }
  )
  val csrCtr = Mux(io.in.valid, control.csrCtr, CSRCtr.csrNone.value.U)
  csr.io.in.csrCtr  := csrCtr
  csr.io.in.csrAddr := aluBSrc(11, 0)
  csr.io.in.csrIn   := csrSrc
  csr.io.in.pc      := jumpASrc

  io.in.ready                := !io.in.valid || io.out.fire
  io.out.valid               := io.in.valid && alu.io.out.valid
  io.out.bits.wa             := in.wa
  io.out.bits.alu_csr_out    := Mux(control.wbSrc(1), csr.io.out.csrout, alu.io.out.bits.result)
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

  if (cpuConfig.sim) {
    io.out.bits.state.get          := in.state.get
    io.out.bits.state.get.exuCycle := io.curCycle.get
    io.out.bits.state.get.npc      := nextPC
    io.out.bits.state.get.csrState := csr.io.csrState.get
  }
}
