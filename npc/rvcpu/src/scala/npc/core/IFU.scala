package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(cpuConfig: CPUConfig) extends Bundle {
  val pc    = UInt(cpuConfig.xlen.W)
  val state = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class IFUIO(cpuConfig: CPUConfig) extends Bundle {
  val out    = DecoupledIO(new IFUOut(cpuConfig))
  val jump   = Input(Bool())
  val nextPC = Input(UInt(cpuConfig.xlen.W))

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
}

class IFU(cpuConfig: CPUConfig) extends Module {
  val io = IO(new IFUIO(cpuConfig))

  val pc  = RegInit(cpuConfig.PCReset.U)
  val npc = Mux(io.jump, io.nextPC, pc)
  pc := Mux(io.out.fire, npc + 4.U, npc)

  /* IO bind */
  io.out.valid   := true.B
  io.out.bits.pc := npc

  if (cpuConfig.sim) {
    io.out.bits.state.get          := DontCare
    io.out.bits.state.get.ifuCycle := io.curCycle.get
    io.out.bits.state.get.pc       := npc
  }
}
