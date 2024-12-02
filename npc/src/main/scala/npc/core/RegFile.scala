package rvcpu.core

import chisel3._
import chisel3.util._

class RegFileAccess(xlen: Int, awidth: Int) extends Bundle {
  val ra1 = Input(UInt(awidth.W))
  val ra2 = Input(UInt(awidth.W))
  val wa  = Input(UInt(awidth.W))
  val we  = Input(Bool())
  val wd  = Input(UInt(xlen.W))
}

class RegFileReturn(xlen: Int) extends Bundle {
  val rd1 = Output(UInt(xlen.W))
  val rd2 = Output(UInt(xlen.W))
}

class RegFileIO(xlen: Int = 32, awidth: Int = 4) extends Bundle {
  val in  = new RegFileAccess(xlen, awidth)
  val out = new RegFileReturn(xlen)
}

class RegFile(xlen: Int = 32, addrWidth: Int = 4) extends Module {
  require(xlen >= 32 && addrWidth > 0)
  val regNum = 1 << addrWidth
  val io     = IO(new RegFileIO(xlen, addrWidth))

  val reg = WireDefault(0.U(xlen.W)) +: Seq.fill(regNum - 1)(Reg(UInt(xlen.W)))
  for (i <- 1 until regNum) {
    reg(i) := Mux(io.in.we && io.in.wa === i.U, io.in.wd, reg(i))
  }

  val regLookup = (0 until regNum).map { i => i.U -> reg(i) }.toSeq
  io.out.rd1 := MuxLookup(io.in.ra1, reg(0))(regLookup)
  io.out.rd2 := MuxLookup(io.in.ra2, reg(0))(regLookup)
}
