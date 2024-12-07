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

class AXI4_TO_AXI4Lite_IPIO extends Bundle {
  val aclk    = Input(Clock())
  val aresetn = Input(Bool())

  val s_axi_awaddr   = Input(UInt(32.W))
  val s_axi_awlen    = Input(UInt(8.W))
  val s_axi_awsize   = Input(UInt(3.W))
  val s_axi_awburst  = Input(UInt(2.W))
  val s_axi_awlock   = Input(Bool())
  val s_axi_awcache  = Input(UInt(4.W))
  val s_axi_awprot   = Input(UInt(3.W))
  val s_axi_awregion = Input(UInt(4.W))
  val s_axi_awqos    = Input(UInt(4.W))
  val s_axi_awvalid  = Input(Bool())
  val s_axi_awready  = Output(Bool())
  val s_axi_wdata    = Input(UInt(32.W))
  val s_axi_wstrb    = Input(UInt(4.W))
  val s_axi_wlast    = Input(Bool())
  val s_axi_wvalid   = Input(Bool())
  val s_axi_wready   = Output(Bool())
  val s_axi_bresp    = Output(UInt(2.W))
  val s_axi_bvalid   = Output(Bool())
  val s_axi_bready   = Input(Bool())
  val s_axi_araddr   = Input(UInt(32.W))
  val s_axi_arlen    = Input(UInt(8.W))
  val s_axi_arsize   = Input(UInt(3.W))
  val s_axi_arburst  = Input(UInt(2.W))
  val s_axi_arlock   = Input(Bool())
  val s_axi_arcache  = Input(UInt(4.W))
  val s_axi_arprot   = Input(UInt(3.W))
  val s_axi_arregion = Input(UInt(4.W))
  val s_axi_arqos    = Input(UInt(4.W))
  val s_axi_arvalid  = Input(Bool())
  val s_axi_arready  = Output(Bool())
  val s_axi_rdata    = Output(UInt(32.W))
  val s_axi_rresp    = Output(UInt(2.W))
  val s_axi_rlast    = Output(Bool())
  val s_axi_rvalid   = Output(Bool())
  val s_axi_rready   = Input(Bool())

  val m_axi_awaddr  = Output(UInt(32.W))
  val m_axi_awprot  = Output(UInt(3.W))
  val m_axi_awvalid = Output(Bool())
  val m_axi_awready = Input(Bool())
  val m_axi_wdata   = Output(UInt(32.W))
  val m_axi_wstrb   = Output(UInt(4.W))
  val m_axi_wvalid  = Output(Bool())
  val m_axi_wready  = Input(Bool())
  val m_axi_bresp   = Input(UInt(2.W))
  val m_axi_bvalid  = Input(Bool())
  val m_axi_bready  = Output(Bool())
  val m_axi_araddr  = Output(UInt(32.W))
  val m_axi_arprot  = Output(UInt(3.W))
  val m_axi_arvalid = Output(Bool())
  val m_axi_arready = Input(Bool())
  val m_axi_rdata   = Input(UInt(32.W))
  val m_axi_rresp   = Input(UInt(2.W))
  val m_axi_rvalid  = Input(Bool())
  val m_axi_rready  = Output(Bool())
}

class AXI4_TO_AXI4Lite_IP extends BlackBox {
  val io = IO(new AXI4_TO_AXI4Lite_IPIO)
}

class AXI4_UART_IP extends Module {
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
  io.interrupt          := uart.io.interrupt
  uart.io.rx            := io.rx
  io.tx                 := uart.io.tx
  val AXI4_TO_AXI4Lite = Module(new AXI4_TO_AXI4Lite_IP)
  AXI4_TO_AXI4Lite.io.aclk    := clock
  AXI4_TO_AXI4Lite.io.aresetn := !reset.asBool

  AXI4_TO_AXI4Lite.io.s_axi_awaddr   := io.axi.awaddr
  AXI4_TO_AXI4Lite.io.s_axi_awlen    := io.axi.awlen
  AXI4_TO_AXI4Lite.io.s_axi_awsize   := io.axi.awsize
  AXI4_TO_AXI4Lite.io.s_axi_awburst  := io.axi.awburst
  AXI4_TO_AXI4Lite.io.s_axi_awlock   := true.B
  AXI4_TO_AXI4Lite.io.s_axi_awcache  := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_awprot   := "b010".U
  AXI4_TO_AXI4Lite.io.s_axi_awregion := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_awqos    := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_awvalid  := io.axi.awvalid
  io.axi.awready                     := AXI4_TO_AXI4Lite.io.s_axi_awready

  AXI4_TO_AXI4Lite.io.s_axi_wdata  := io.axi.wdata
  AXI4_TO_AXI4Lite.io.s_axi_wstrb  := io.axi.wstrb
  AXI4_TO_AXI4Lite.io.s_axi_wlast  := io.axi.wlast
  AXI4_TO_AXI4Lite.io.s_axi_wvalid := io.axi.wvalid
  io.axi.wready                    := AXI4_TO_AXI4Lite.io.s_axi_wready

  io.axi.bresp                     := AXI4_TO_AXI4Lite.io.s_axi_bresp
  io.axi.bvalid                    := AXI4_TO_AXI4Lite.io.s_axi_bvalid
  AXI4_TO_AXI4Lite.io.s_axi_bready := io.axi.bready

  AXI4_TO_AXI4Lite.io.s_axi_araddr   := io.axi.araddr
  AXI4_TO_AXI4Lite.io.s_axi_arlen    := io.axi.arlen
  AXI4_TO_AXI4Lite.io.s_axi_arsize   := io.axi.arsize
  AXI4_TO_AXI4Lite.io.s_axi_arburst  := io.axi.arburst
  AXI4_TO_AXI4Lite.io.s_axi_arlock   := true.B
  AXI4_TO_AXI4Lite.io.s_axi_arcache  := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_arprot   := "b010".U
  AXI4_TO_AXI4Lite.io.s_axi_arregion := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_arqos    := "b0000".U
  AXI4_TO_AXI4Lite.io.s_axi_arvalid  := io.axi.arvalid

  io.axi.arready                   := AXI4_TO_AXI4Lite.io.s_axi_arready
  io.axi.rdata                     := AXI4_TO_AXI4Lite.io.s_axi_rdata
  io.axi.rresp                     := AXI4_TO_AXI4Lite.io.s_axi_rresp
  io.axi.rlast                     := AXI4_TO_AXI4Lite.io.s_axi_rlast
  io.axi.rvalid                    := AXI4_TO_AXI4Lite.io.s_axi_rvalid
  AXI4_TO_AXI4Lite.io.s_axi_rready := io.axi.rready

  uart.io.s_axi_awaddr              := AXI4_TO_AXI4Lite.io.m_axi_awaddr
  uart.io.s_axi_awvalid             := AXI4_TO_AXI4Lite.io.m_axi_awvalid
  AXI4_TO_AXI4Lite.io.m_axi_awready := uart.io.s_axi_awready

  uart.io.s_axi_wdata              := AXI4_TO_AXI4Lite.io.m_axi_wdata
  uart.io.s_axi_wstrb              := AXI4_TO_AXI4Lite.io.m_axi_wstrb
  uart.io.s_axi_wvalid             := AXI4_TO_AXI4Lite.io.m_axi_wvalid
  AXI4_TO_AXI4Lite.io.m_axi_wready := uart.io.s_axi_wready

  AXI4_TO_AXI4Lite.io.m_axi_bresp  := uart.io.s_axi_bresp
  AXI4_TO_AXI4Lite.io.m_axi_bvalid := uart.io.s_axi_bvalid
  uart.io.s_axi_bready             := AXI4_TO_AXI4Lite.io.m_axi_bready

  uart.io.s_axi_araddr              := AXI4_TO_AXI4Lite.io.m_axi_araddr
  uart.io.s_axi_arvalid             := AXI4_TO_AXI4Lite.io.m_axi_arvalid
  AXI4_TO_AXI4Lite.io.m_axi_arready := uart.io.s_axi_arready

  AXI4_TO_AXI4Lite.io.m_axi_rdata  := uart.io.s_axi_rdata
  AXI4_TO_AXI4Lite.io.m_axi_rresp  := uart.io.s_axi_rresp
  AXI4_TO_AXI4Lite.io.m_axi_rvalid := uart.io.s_axi_rvalid
  uart.io.s_axi_rready             := AXI4_TO_AXI4Lite.io.m_axi_rready

  io.axi.bid := DontCare
  io.axi.rid := DontCare
}
