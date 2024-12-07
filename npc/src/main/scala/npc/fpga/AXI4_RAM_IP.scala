package rvcpu.fpga

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._

import chisel3._
import chisel3.util._
import scala.collection.immutable.RedBlackTree

class RAM_IPIO extends Bundle {
  val s_aclk        = Input(Clock())
  val s_aresetn     = Input(Bool())
  val s_axi_awid    = Input(UInt(4.W))
  val s_axi_awaddr  = Input(UInt(32.W))
  val s_axi_awlen   = Input(UInt(8.W))
  val s_axi_awsize  = Input(UInt(3.W))
  val s_axi_awburst = Input(UInt(2.W))
  val s_axi_awvalid = Input(Bool())
  val s_axi_awready = Output(Bool())

  val s_axi_wdata  = Input(UInt(32.W))
  val s_axi_wstrb  = Input(UInt(4.W))
  val s_axi_wlast  = Input(Bool())
  val s_axi_wvalid = Input(Bool())
  val s_axi_wready = Output(Bool())

  val s_axi_bid    = Output(UInt(4.W))
  val s_axi_bresp  = Output(UInt(2.W))
  val s_axi_bvalid = Output(Bool())
  val s_axi_bready = Input(Bool())

  val s_axi_arid    = Input(UInt(4.W))
  val s_axi_araddr  = Input(UInt(32.W))
  val s_axi_arlen   = Input(UInt(8.W))
  val s_axi_arsize  = Input(UInt(3.W))
  val s_axi_arburst = Input(UInt(2.W))
  val s_axi_arvalid = Input(Bool())
  val s_axi_arready = Output(Bool())

  val s_axi_rid    = Output(UInt(4.W))
  val s_axi_rdata  = Output(UInt(32.W))
  val s_axi_rresp  = Output(UInt(2.W))
  val s_axi_rlast  = Output(Bool())
  val s_axi_rvalid = Output(Bool())
  val s_axi_rready = Input(Bool())
}

class RAM_IP extends BlackBox {
  val io = IO(new RAM_IPIO)
}

class AXI4_RAM_IP extends Module {
  val awidth = 32
  val dwidth = 32
  val size   = dwidth / 8
  val io     = IO(Flipped(new AXI4MasterIO))
  val Mem    = Module(new RAM_IP)
  Mem.io.s_aclk    := clock
  Mem.io.s_aresetn := !reset.asBool

  Mem.io.s_axi_awid    := io.awid
  Mem.io.s_axi_awaddr  := io.awaddr
  Mem.io.s_axi_awlen   := io.awlen
  Mem.io.s_axi_awsize  := io.awsize
  Mem.io.s_axi_awburst := io.awburst
  Mem.io.s_axi_awvalid := io.awvalid
  io.awready           := Mem.io.s_axi_awready

  Mem.io.s_axi_wdata  := io.wdata
  Mem.io.s_axi_wstrb  := io.wstrb
  Mem.io.s_axi_wlast  := io.wlast
  Mem.io.s_axi_wvalid := io.wvalid
  io.wready           := Mem.io.s_axi_wready

  io.bid              := Mem.io.s_axi_bid
  io.bresp            := Mem.io.s_axi_bresp
  io.bvalid           := Mem.io.s_axi_bvalid
  Mem.io.s_axi_bready := io.bready

  Mem.io.s_axi_arid    := io.arid
  Mem.io.s_axi_araddr  := io.araddr
  Mem.io.s_axi_arlen   := io.arlen
  Mem.io.s_axi_arsize  := io.arsize
  Mem.io.s_axi_arburst := io.arburst
  Mem.io.s_axi_arvalid := io.arvalid
  io.arready           := Mem.io.s_axi_arready

  io.rid              := Mem.io.s_axi_rid
  io.rdata            := Mem.io.s_axi_rdata
  io.rresp            := Mem.io.s_axi_rresp
  io.rlast            := Mem.io.s_axi_rlast
  io.rvalid           := Mem.io.s_axi_rvalid
  Mem.io.s_axi_rready := io.rready
}
