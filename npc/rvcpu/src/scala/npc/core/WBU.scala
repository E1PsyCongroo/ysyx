package rvcpu.core

import chisel3._
import chisel3.util._

class WBUOut(cpuConfig: CPUConfig) extends Bundle {
  val state = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class WBUIO(cpuConfig: CPUConfig) extends Bundle {
  val in            = Flipped(DecoupledIO(new LSUOut(cpuConfig)))
  val out           = DecoupledIO(new WBUOut(cpuConfig))
  val RegFileAccess = Flipped(new RegFileAccess(cpuConfig))

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
  val gprState = if (cpuConfig.sim) Some(Input(new GPRState(cpuConfig))) else None
}

class WBU(cpuConfig: CPUConfig) extends Module {
  val io = IO(new WBUIO(cpuConfig))

  val in      = WireDefault(io.in.bits)
  val wa      = in.wa
  val wd      = in.wd
  val control = in.control

  io.in.ready          := !io.in.valid || io.out.fire
  io.out.valid         := io.in.valid
  io.RegFileAccess.ra1 := DontCare
  io.RegFileAccess.ra2 := DontCare
  io.RegFileAccess.wa  := wa
  io.RegFileAccess.we  := control.regWe && io.in.valid
  io.RegFileAccess.wd  := wd

  if (cpuConfig.sim) {
    io.out.bits.state.get          := in.state.get
    io.out.bits.state.get.wbuCycle := io.curCycle.get
    io.out.bits.state.get.gprState := io.gprState.get
  }
}
