package rvcpu.fpga

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._

import chisel3._
import chisel3.util._

class CLK_WIZIO extends Bundle {
  val clk_out1 = Output(Clock()) // 200 MHz
  val clk_out2 = Output(Clock()) // 100 MHz
  val clk_out3 = Output(Clock()) // 50  MHz
  val clk_out4 = Output(Clock()) // 40  MHz
  val clk_out5 = Output(Clock()) // 25  MHz
  val reset    = Input(Reset())
  val locked   = Output(Bool())
  val clk_in1  = Input(Clock())
}

class CLK_WIZ extends BlackBox {
  val io = IO(new CLK_WIZIO)
}

class PLL_IP extends Module {
  val io = IO(new Bundle {
    val locked = Output(Bool())
    val clk    = Output(Clock())
  })
  val pll = Module(new CLK_WIZ)
  pll.io.clk_in1 := clock
  pll.io.reset   := reset
  io.locked      := pll.io.locked
  io.clk         := pll.io.clk_out3
}
