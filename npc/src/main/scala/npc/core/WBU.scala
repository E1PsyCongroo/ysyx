package rvcpu.core

import chisel3._
import chisel3.util._

class WBUOut(xlen: Int, sim: Boolean) extends Bundle {
  val nextPC     = if (sim) Some(Output(UInt(xlen.W))) else None
  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val memAddr    = if (sim) Some(Output(UInt(32.W))) else None
  val memAccess  = if (sim) Some(Output(Bool())) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
  val mtvec      = if (sim) Some(Output(UInt(xlen.W))) else None
  val mepc       = if (sim) Some(Output(UInt(xlen.W))) else None
}

class WBUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in            = Flipped(DecoupledIO(new LSUOut(xlen, extentionE, sim)))
  val out           = DecoupledIO(new WBUOut(xlen, sim))
  val RegFileAccess = Flipped(new RegFileAccess(xlen, if (extentionE) 4 else 5))
}

class WBU(xlen: Int, extentionE: Boolean, sim: Boolean) extends Module {
  val io = IO(new WBUIO(xlen, extentionE, sim))

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
  if (sim) {
    io.out.bits.nextPC.get     := in.nextPC.get
    io.out.bits.inst.get       := in.inst.get
    io.out.bits.memAddr.get    := in.memAddr.get
    io.out.bits.memAccess.get  := in.memAccess.get
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := in.isEnd.get
    io.out.bits.exitCode.get   := in.exitCode.get
    io.out.bits.mtvec.get      := in.mtvec.get
    io.out.bits.mepc.get       := in.mepc.get
  }
}
