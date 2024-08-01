package RV32E

import chisel3._
import chisel3.util._

class MemIO extends Bundle {
  val clock = Input(Clock())
  val valid = Input(Bool())
  val raddr = Input(UInt(32.W))
  val rdata = Output(UInt(32.W))

  val waddr = Input(UInt(32.W))
  val wdata = Input(UInt(32.W))
  val wmask = Input(UInt(4.W))
  val wen = Input(Bool())
}

class Mem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO)
  addResource("/src/main/resource/Mem.sv")
}