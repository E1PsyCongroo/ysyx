package RVCPU.util

import chisel3._
import chisel3.util._

class LSFR extends Module {
  val io = IO(new Bundle {
    val next = Input(Bool())
    val out = Output(UInt(8.W))
  })
  val reg    = RegInit(1.U(8.W))
  val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
  reg    := Mux(io.next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
  io.out := reg
}

object LSFR {
  def apply(next: Bool) = {
    val lsfr = Module(new LSFR)
    lsfr.io.next  := next
    lsfr.io.out
  }
}
