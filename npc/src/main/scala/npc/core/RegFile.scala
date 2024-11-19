package rvcpu.core

import chisel3._
import chisel3.util._

class RegFileIO(xlen: Int = 32, addrWidth: Int = 4) extends Bundle {
  val ra1 = Input(UInt(addrWidth.W))
  val ra2 = Input(UInt(addrWidth.W))
  val rd1 = Output(UInt(xlen.W))
  val rd2 = Output(UInt(xlen.W))
  val wa  = Input(UInt(addrWidth.W))
  val we  = Input(Bool())
  val wd  = Input(UInt(xlen.W))
}

class RegFile(xlen: Int = 32, addrWidth: Int = 4) extends Module {
  require(xlen >= 32 && addrWidth > 0)
  val regNum = 1 << addrWidth
  val io     = IO(new RegFileIO(xlen, addrWidth))

  val reg = WireDefault(0.U(xlen.W)) +: Seq.fill(regNum - 1)(Reg(UInt(xlen.W)))
  for (i <- 1 until regNum) {
    reg(i) := Mux(io.we && io.wa === i.U, io.wd, reg(i))
  }

  val regLookup = (0 until regNum).map { i => i.U -> reg(i) }.toSeq
  io.rd1 := MuxLookup(io.ra1, 0.U)(regLookup)
  io.rd2 := MuxLookup(io.ra2, 0.U)(regLookup)
}
