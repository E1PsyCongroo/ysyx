package rvcpu.core

import chisel3._
import chisel3.util._

class IDUOut(cpuConfig: CPUConfig) extends Bundle {
  val aluASrc  = UInt(cpuConfig.xlen.W)
  val aluBSrc  = UInt(cpuConfig.xlen.W)
  val jumpASrc = UInt(cpuConfig.xlen.W)
  val jumpBSrc = UInt(cpuConfig.xlen.W)
  val ra1      = UInt(if (cpuConfig.extentionE) 4.W else 5.W)
  val wa       = UInt(if (cpuConfig.extentionE) 4.W else 5.W)
  val control = new Bundle {
    val regWe      = Bool()
    val wbSrc      = UInt(WBSrcFrom.getWidth.W)
    val aluSel     = UInt(ALUOutSel.getWidth.W)
    val isArith    = Bool()
    val isLeft     = Bool()
    val isUnsigned = Bool()
    val isSub      = Bool()
    val csrSrc     = UInt(CSRSrcFrom.getWidth.W)
    val csrCtr     = UInt(CSRCtr.getWidth.W)
    val brType     = UInt(BrType.getWidth.W)
    val pcSrc      = UInt(PCSrcFrom.getWidth.W)
    val memRen     = Bool()
    val memWen     = Bool()
    val memOp      = UInt(MemOp.getWidth.W)
  }

  val state = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class IDUIO(cpuConfig: CPUConfig) extends Bundle {
  val in            = Flipped(DecoupledIO(new ICacheOut(cpuConfig)))
  val out           = DecoupledIO(new IDUOut(cpuConfig))
  val stall         = Input(Bool())
  val RegFileAccess = Flipped(new RegFileAccess(cpuConfig))
  val RegFileReturn = Flipped(new RegFileReturn(cpuConfig))
  val fence_i       = Output(Bool())

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
}

class IDU(cpuConfig: CPUConfig) extends Module {
  val io = IO(new IDUIO(cpuConfig))

  val in = WireDefault(io.in.bits)

  val instruction = in.instruction
  val ra1         = instruction(19, 15)
  val ra2         = instruction(24, 20)
  val wa          = instruction(11, 7)
  val pc          = in.pc
  val rd1         = io.RegFileReturn.rd1
  val rd2         = io.RegFileReturn.rd2
  val control     = Module(new Control(cpuConfig))
  val imm         = Module(new ImmGen(cpuConfig))

  control.io.instruction := instruction
  imm.io.instruction     := instruction
  imm.io.immType         := control.io.control.immType

  if (cpuConfig.sim) {
    io.RegFileAccess.ra1 := Mux(control.io.control.isEnd.get, 10.U, ra1) & Fill(5, control.io.control.needRd1)
  } else {
    io.RegFileAccess.ra1 := ra1 & Fill(5, control.io.control.needRd1)
  }
  io.RegFileAccess.ra2 := ra2 & Fill(5, control.io.control.needRd2)
  io.RegFileAccess.wa  := DontCare
  io.RegFileAccess.we  := DontCare
  io.RegFileAccess.wd  := DontCare

  val ifenced = RegNext(!io.out.fire && io.fence_i)

  val aluASrc = MuxCase(
    DontCare,
    Seq(
      ALUASrcFrom.fromPc -> pc,
      ALUASrcFrom.fromRs1 -> rd1
    ).map { case (key, data) => (control.io.control.aluASrc === key, data) }
  )
  val aluBSrc = MuxCase(
    DontCare,
    Seq(
      ALUBSrcFrom.from4 -> 4.U,
      ALUBSrcFrom.fromRs2 -> rd2,
      ALUBSrcFrom.fromImm -> imm.io.imm
    ).map { case (key, data) => (control.io.control.aluBSrc === key, data) }
  )
  val jumpASrc = MuxCase(
    DontCare,
    Seq(
      JumpASrcFrom.fromPc -> pc,
      JumpASrcFrom.fromRs1 -> rd1
    ).map { case (key, data) => (control.io.control.jumpASrc === key, data) }
  )
  val jumpBSrc = MuxCase(
    DontCare,
    Seq(
      JumpBSrcFrom.fromRs2 -> rd2,
      JumpBSrcFrom.fromImm -> imm.io.imm
    ).map { case (key, data) => (control.io.control.jumpBSrc === key, data) }
  )

  io.in.ready                    := !io.in.valid || io.out.fire
  io.out.valid                   := io.in.valid && !io.stall
  io.out.bits.aluASrc            := aluASrc
  io.out.bits.aluBSrc            := aluBSrc
  io.out.bits.jumpASrc           := jumpASrc
  io.out.bits.jumpBSrc           := jumpBSrc
  io.out.bits.wa                 := wa
  io.out.bits.ra1                := ra1
  io.out.bits.control.regWe      := control.io.control.regWe
  io.out.bits.control.wbSrc      := control.io.control.wbSrc
  io.out.bits.control.aluSel     := control.io.control.aluSel
  io.out.bits.control.isArith    := control.io.control.isArith
  io.out.bits.control.isLeft     := control.io.control.isLeft
  io.out.bits.control.isUnsigned := control.io.control.isUnsigned
  io.out.bits.control.isSub      := control.io.control.isSub
  io.out.bits.control.csrSrc     := control.io.control.csrSrc
  io.out.bits.control.csrCtr     := control.io.control.csrCtr
  io.out.bits.control.brType     := control.io.control.brType
  io.out.bits.control.pcSrc      := control.io.control.pcSrc
  io.out.bits.control.memRen     := control.io.control.memRen
  io.out.bits.control.memWen     := control.io.control.memWen
  io.out.bits.control.memOp      := control.io.control.memOp
  io.fence_i                     := io.in.valid && control.io.control.fence_i && !ifenced

  if (cpuConfig.sim) {
    io.out.bits.state.get          := in.state.get
    io.out.bits.state.get.iduCycle := io.curCycle.get
    io.out.bits.state.get.isEnd    := control.io.control.isEnd.get
    io.out.bits.state.get.exitCode := rd1
  }
}
