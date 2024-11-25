package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int, sim: Boolean) extends Bundle {
  val pc = Output(UInt(xlen.W))
}

class IFUIO(xlen: Int, sim: Boolean) extends Bundle {
  val out    = DecoupledIO(new IFUOut(xlen, sim))
  val jump   = Input(Bool())
  val nextPC = Input(UInt(xlen.W))
}

class IFU(
  xlen:    Int,
  PCReset: BigInt,
  sim:     Boolean = true)
    extends Module {
  val io = IO(new IFUIO(xlen, sim))

  val pc = RegInit(PCReset.U)
  pc := Mux(io.jump, io.nextPC, Mux(io.out.fire, pc + 4.U, pc))

  /* IO bind */
  io.out.valid   := true.B
  io.out.bits.pc := pc
}
