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
  val in        = Flipped(DecoupledIO(new WBUOut(xlen)))
  val out       = DecoupledIO(new IFUOut(xlen))
  val ICacheIn  = DecoupledIO(new ICacheIn(awidth))
  val ICacheOut = Flipped(DecoupledIO(new ICacheOut(xlen)))
}

class IFU(awidth: Int, xlen: Int) extends Module {
  val io = IO(new IFUIO(awidth, xlen))

  val pc          = WireDefault(io.in.bits.nextPc)
  val instruction = WireDefault(io.ICacheOut.bits.rdata)

  val sIdle :: sSetAddr :: sFetch :: sExec :: Nil = Enum(4)

  val state     = RegInit(sIdle)
  val isIdle    = state === sIdle
  val isSetAddr = state === sSetAddr
  val isFetch   = state === sFetch
  val isExec    = state === sExec

  state := MuxLookup(state, sSetAddr)(
    Seq(
      sIdle -> Mux(io.in.fire, sSetAddr, sIdle),
      sSetAddr -> Mux(io.ICacheIn.fire, sFetch, sSetAddr),
      sFetch -> Mux(io.ICacheOut.fire, sExec, sFetch),
      sExec -> Mux(io.out.fire, sIdle, sExec)
    )
  )

  /* IO bind */
  io.ICacheIn.valid      := isSetAddr
  io.ICacheIn.bits.raddr := pc
  io.ICacheOut.ready     := isFetch

  io.in.ready             := isIdle
  io.out.valid            := isExec
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction
}
