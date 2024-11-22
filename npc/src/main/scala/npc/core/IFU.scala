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
  val in     = Flipped(DecoupledIO(new WBUOut(xlen)))
  val out    = DecoupledIO(new IFUOut(xlen))
  val flush  = Input(Bool())
  val master = new AXI4MasterIO
}

class IFU(
  awidth:             Int,
  xlen:               Int,
  totalWidth:         Int     = 4,
  blockWidth:         Int     = 2,
  associativityWidth: Int     = 0,
  needCache:          UInt => Bool,
  supportBurst:       UInt => Bool,
  sim:                Boolean = true)
    extends Module {
  val io = IO(new IFUIO(awidth, xlen) {
    val ICacheTrace = if (sim) Some(new ICacheTrace) else None
  })

  val pc = WireDefault(io.in.bits.nextPc)

  val ICache = Module(
    new ICache(awidth, xlen, totalWidth, blockWidth, associativityWidth, needCache, supportBurst, sim)
  )
  ICache.io.flush := io.flush

  val instruction = WireDefault(ICache.io.out.bits.rdata)

  /* IO bind */
  ICache.io.in.valid      := io.in.valid
  io.in.ready             := ICache.io.in.ready
  ICache.io.in.bits.raddr := pc

  io.out.valid            := ICache.io.out.valid
  ICache.io.out.ready     := io.out.ready
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction

  io.master <> ICache.io.master

  if (sim) {
    io.ICacheTrace.get <> ICache.io.trace.get
  }
}
