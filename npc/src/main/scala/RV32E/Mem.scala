package RVCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._


class MemIO extends Bundle {
  val ren     = Input(Bool())
  val raddr   = Input(UInt(32.W))
  val rdata   = Output(UInt(32.W))

  val wen     = Input(Bool())
  val waddr   = Input(UInt(32.W))
  val wdata   = Input(UInt(32.W))
  val wmask   = Input(UInt(4.W))
}

class Mem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO{
    val clock = Input(Clock())
  })
  addResource("/Mem.sv")
}

class AXILiteMem extends Module {
}

