package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
}

class IFUIO(awidth: Int, xlen: Int) extends Bundle {
  val out        = DecoupledIO(new IFUOut(xlen))
  val jump       = Input(Bool())
  val nextPC     = Input(UInt(xlen.W))
  val cacheFlush = Input(Bool())
  val master     = new AXI4MasterIO
}

class IFU(
  awidth:             Int,
  xlen:               Int,
  PCReset:            BigInt,
  totalWidth:         Int     = 4,
  blockWidth:         Int     = 2,
  associativityWidth: Int     = 0,
  needCache:          UInt => Bool,
  sim:                Boolean = true)
    extends Module {
  val io = IO(new IFUIO(awidth, xlen) {
    val ICacheTrace = if (sim) Some(new ICacheTrace) else None
  })

  val pc = RegInit(PCReset.U)
  pc := Mux(io.jump, io.nextPC, Mux(io.out.fire, pc + 4.U, pc))

  val ICache = Module(
    new ICache(awidth, xlen, totalWidth, blockWidth, associativityWidth, needCache, sim)
  )
  ICache.io.flush := io.cacheFlush

  val instruction = WireDefault(ICache.io.out.bits.rdata)

  /* IO bind */
  ICache.io.in.valid      := true.B
  ICache.io.in.bits.raddr := pc

  ICache.io.out.ready     := io.out.ready
  io.out.valid            := ICache.io.out.valid
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction

  io.master <> ICache.io.master

  if (sim) {
    io.ICacheTrace.get <> ICache.io.trace.get
  }
}
