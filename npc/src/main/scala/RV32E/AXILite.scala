package RVCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class AXILiteIO(xlen: Int = 32) extends Bundle {
  /* Global */
  val aclk    = Input(Clock())
  val aresetn = Input(Reset())

  /* Write address channel */
  val awvalid = Input(Bool())
  val awready = Output(Bool())
  val awid    = Input(UInt(0.W))
  val awaddr  = Input(UInt(xlen.W))
  val awport  = Input(UInt(3.W))

  /* Write data channel */
  val wvalid  = Input(Bool())
  val wready  = Output(Bool())
  val wdata   = Input(UInt(xlen.W))
  val wstarb  = Input(UInt((xlen/8).W))

  /* Write response channel */
  val bvalid  = Output(Bool())
  val bready  = Input(Bool())
  val bid     = Output(UInt(0.W))
  val bresp   = Output(UInt(2.W))

  /* Read address channel */
  val arvalid = Input(Bool())
  val arready = Output(Bool())
  val arid    = Input(UInt(0.W))
  val araddr  = Input(UInt(xlen.W))
  val arport  = Input(UInt(3.W))

  /* Read data channel */
  val rvalid  = Output(Bool())
  val rready  = Input(Bool())
  val rid     = Output(UInt(0.W))
  val rdata   = Output(UInt(xlen.W))
  val rresp   = Output(UInt(2.W))
}
