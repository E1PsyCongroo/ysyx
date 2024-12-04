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

  val pc  = RegInit(PCReset.U)
  val npc = Mux(io.jump, io.nextPC, pc)
  pc := Mux(io.out.fire, npc + 4.U, npc)

  /* IO bind */
  io.out.valid   := true.B
  io.out.bits.pc := npc
}
