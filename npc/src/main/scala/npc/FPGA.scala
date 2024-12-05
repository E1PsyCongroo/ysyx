package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.fpga._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class FPGAIO extends Bundle {
  val rx     = Input(UInt(1.W))
  val tx     = Output(UInt(1.W))
  val locked = Output(Bool())
}

class FPGA(awidth: Int, xlen: Int, extentionE: Boolean, PCReset: BigInt, sim: Boolean) extends Module {
  val io     = IO(new FPGAIO)
  val pll    = Module(new PLL_IP)
  val locked = RegInit(true.B)
  locked := locked && pll.io.locked
  withClockAndReset(pll.io.clk, !pll.io.locked) {
    val npc   = Module(new NPCImpl(awidth, xlen, extentionE, PCReset, sim))
    val clint = Module(new CLINT(awidth, xlen, Dev.fpga.CLINTAddr))
    val uart  = Module(new AXI4_UART_IP(Dev.fpga.UARTAddr))
    val bram  = Module(new AXI4_RAM_IP(Dev.fpga.BRAMAddr))
    AXI4Interconnect(
      Seq(npc.io.master),
      Seq(clint.io, uart.io.axi, bram.io),
      Seq(Dev.fpga.CLINTAddr.in, Dev.fpga.UARTAddr.in, Dev.fpga.BRAMAddr.in)
    )
    npc.io.slave <> AXI4.none
    npc.io.interrupt := uart.io.interrupt
    uart.io.rx       := io.rx
    io.tx            := uart.io.tx
  }
  io.locked := locked
}
