package rvcpu.core

import chisel3._
import chisel3.util._

class RegFileAccess(cpuConfig: CPUConfig) extends Bundle {
  val ra1 = Input(UInt(if (cpuConfig.extentionE) 4.W else 5.W))
  val ra2 = Input(UInt(if (cpuConfig.extentionE) 4.W else 5.W))
  val wa  = Input(UInt(if (cpuConfig.extentionE) 4.W else 5.W))
  val we  = Input(Bool())
  val wd  = Input(UInt(cpuConfig.xlen.W))
}

class RegFileReturn(cpuConfig: CPUConfig) extends Bundle {
  val rd1 = Output(UInt(cpuConfig.xlen.W))
  val rd2 = Output(UInt(cpuConfig.xlen.W))
}

class GPRState(cpuConfig: CPUConfig) extends Bundle {
  val gprs = Vec(if (cpuConfig.extentionE) 16 else 32, UInt(cpuConfig.xlen.W))
}

class RegFileIO(cpuConfig: CPUConfig) extends Bundle {
  val in  = new RegFileAccess(cpuConfig)
  val out = new RegFileReturn(cpuConfig)

  val gprState = if (cpuConfig.sim) Some(Output(new GPRState(cpuConfig))) else None
}

class RegFile(cpuConfig: CPUConfig) extends Module {
  val regNum = if (cpuConfig.extentionE) 16 else 32
  val io     = IO(new RegFileIO(cpuConfig))

  val reg     = WireDefault(0.U(cpuConfig.xlen.W)) +: Seq.fill(regNum - 1)(Reg(UInt(cpuConfig.xlen.W)))
  val nextReg = Seq.fill(regNum)(Wire(UInt(cpuConfig.xlen.W)))
  nextReg(0) := 0.U
  for (i <- 1 until regNum) {
    nextReg(i) := Mux(io.in.we && io.in.wa === i.U, io.in.wd, reg(i))
    reg(i)     := nextReg(i)
  }

  val regLookup = (0 until regNum).map { i => i.U -> reg(i) }.toSeq
  io.out.rd1 := MuxLookup(io.in.ra1, reg(0))(regLookup)
  io.out.rd2 := MuxLookup(io.in.ra2, reg(0))(regLookup)

  if (cpuConfig.sim) {
    for (i <- 0 until regNum) {
      io.gprState.get.gprs(i) := nextReg(i)
    }
  }
}
