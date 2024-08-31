package rvcpu.core

import chisel3._
import chisel3.util._

class RegFileIO(xlen: Int = 32, addrWidth: Int = 4) extends Bundle {
  val ra1 = Input(UInt(addrWidth.W))
  val ra2 = Input(UInt(addrWidth.W))
  val rd1 = Output(UInt(xlen.W))
  val rd2 = Output(UInt(xlen.W))
  val wa = Input(UInt(addrWidth.W))
  val we = Input(Bool())
  val wd = Input(UInt(xlen.W))
}

class RegFile(xlen: Int = 32, addrWidth: Int = 4) extends Module {
  require(xlen >= 32 && addrWidth > 0)
  val io = IO(new RegFileIO(xlen, addrWidth))
  val reg = RegInit(VecInit(Seq.fill(1 << addrWidth)(0.U(xlen.W))))
  io.rd1 := Mux(io.ra1.orR, reg(io.ra1), 0.U)
  io.rd2 := Mux(io.ra2.orR, reg(io.ra2), 0.U)
  reg(io.wa) := Mux(io.we && io.wa.orR, io.wd, reg(io.wa))
}
