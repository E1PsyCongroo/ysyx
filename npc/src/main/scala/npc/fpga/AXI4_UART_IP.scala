package rvcpu.fpga

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._

import chisel3._
import chisel3.util._

class AXI_UART_IPIO extends Bundle {
  val s_axi_aclk    = Input(Clock())
  val s_axi_aresetn = Input(Bool())

  val s_axi_awaddr  = Input(UInt(4.W))
  val s_axi_awvalid = Input(Bool())
  val s_axi_awready = Output(Bool())

  val s_axi_wdata  = Input(UInt(32.W))
  val s_axi_wstrb  = Input(UInt(4.W))
  val s_axi_wvalid = Input(Bool())
  val s_axi_wready = Output(Bool())

  val s_axi_bresp  = Output(UInt(2.W))
  val s_axi_bvalid = Output(Bool())
  val s_axi_bready = Input(Bool())

  val s_axi_araddr  = Input(UInt(4.W))
  val s_axi_arvalid = Input(Bool())
  val s_axi_arready = Output(Bool())

  val s_axi_rdata  = Output(UInt(32.W))
  val s_axi_rresp  = Output(UInt(2.W))
  val s_axi_rvalid = Output(Bool())
  val s_axi_rready = Input(Bool())

  val rx        = Input(UInt(1.W))
  val tx        = Output(UInt(1.W))
  val interrupt = Output(Bool())
}

class AXI_UART_IP extends BlackBox {
  val io = IO(new AXI_UART_IPIO)
}

class AXI4_UART_IP(area: Area) extends Module {
  val awidth = 32
  val dwidth = 32
  val size   = dwidth / 8
  val io = IO(new Bundle {
    val axi       = Flipped(new AXI4MasterIO)
    val interrupt = Output(Bool())
    val rx        = Input(UInt(1.W))
    val tx        = Output(UInt(1.W))
  })
  val uart = Module(new AXI_UART_IP)
  uart.io.s_axi_aclk    := clock
  uart.io.s_axi_aresetn := !reset.asBool

  io.axi <> AXI4.none

  uart.io.s_axi_awaddr  := io.axi.awaddr
  uart.io.s_axi_awvalid := io.axi.awvalid
  io.axi.awready        := uart.io.s_axi_awready

  uart.io.s_axi_wdata  := io.axi.wdata
  uart.io.s_axi_wstrb  := io.axi.wstrb
  uart.io.s_axi_wvalid := io.axi.wvalid
  io.axi.wready        := uart.io.s_axi_wready

  io.axi.bresp         := uart.io.s_axi_bresp
  io.axi.bvalid        := uart.io.s_axi_bvalid
  uart.io.s_axi_bready := io.axi.bready

  uart.io.s_axi_araddr  := io.axi.araddr
  uart.io.s_axi_arvalid := io.axi.arvalid
  io.axi.arready        := uart.io.s_axi_arready

  io.axi.rdata         := uart.io.s_axi_rdata
  io.axi.rresp         := uart.io.s_axi_rresp
  io.axi.rvalid        := uart.io.s_axi_rvalid
  uart.io.s_axi_rready := io.axi.rready

  uart.io.rx   := io.rx
  io.tx        := uart.io.tx
  io.interrupt := uart.io.interrupt
}
