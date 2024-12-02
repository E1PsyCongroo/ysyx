package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int) extends Bundle {
  val pc = Output(UInt(xlen.W))
}

class IFUIO(xlen: Int) extends Bundle {
  val out    = DecoupledIO(new IFUOut(xlen))
  val jump   = Input(Bool())
  val nextPC = Input(UInt(xlen.W))
}

class IFU(
  xlen:    Int,
  PCReset: BigInt)
    extends Module {
  val io = IO(new IFUIO(xlen))

  val pc = RegInit(PCReset.U)
  pc := Mux(io.jump, io.nextPC, Mux(io.out.fire, pc + 4.U, pc))

  /* IO bind */
  io.out.valid   := !io.jump
  io.out.bits.pc := pc
}
