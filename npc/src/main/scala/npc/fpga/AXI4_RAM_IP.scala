package rvcpu.fpga

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._

import chisel3._
import chisel3.util._

class RAM_IPIO extends Bundle {
  val clka  = Input(Clock())
  val ena   = Input(Bool())
  val wea   = Input(UInt(4.W))
  val addra = Input(UInt(15.W))
  val dina  = Input(UInt(32.W))

  val clkb  = Input(Clock())
  val enb   = Input(Bool())
  val addrb = Input(UInt(15.W))
  val doutb = Output(UInt(32.W))
}

class RAM_IP extends BlackBox {
  val io = IO(new RAM_IPIO)
}

class AXI4_RAM_IP(area: Area) extends Module {
  val awidth = 32
  val dwidth = 32
  val size   = dwidth / 8
  val io     = IO(Flipped(new AXI4MasterIO))
  val Mem    = Module(new RAM_IP)
  Mem.io.clka := clock
  Mem.io.clkb := clock

  val awfire = io.awvalid && io.awready
  val wfire  = io.wvalid && io.wready
  val bfire  = io.bvalid && io.bready
  val arfire = io.arvalid && io.arready
  val rfire  = io.rvalid && io.rready

  val sWriteIdle :: sSendBresp :: Nil = Enum(2)
  val sReadIdle :: sSendRresp :: Nil  = Enum(2)

  val writeState  = RegInit(sWriteIdle)
  val isWriteIdle = writeState === sWriteIdle
  val isSendBresp = writeState === sSendBresp

  val readState   = RegInit(sReadIdle)
  val isReadIdle  = readState === sReadIdle
  val isSendRresp = readState === sSendRresp

  writeState := MuxLookup(writeState, sWriteIdle)(
    Seq(
      sWriteIdle -> Mux(awfire && wfire, sSendBresp, sWriteIdle),
      sSendBresp -> Mux(bfire, sWriteIdle, sSendBresp)
    )
  )

  readState := MuxLookup(readState, sReadIdle)(
    Seq(
      sReadIdle -> Mux(arfire, sSendRresp, sReadIdle),
      sSendRresp -> Mux(rfire && io.rlast, sReadIdle, sSendRresp)
    )
  )

  /* Write address channel */
  val awready = WireDefault(isWriteIdle)
  val awaddr  = WireDefault(io.awaddr)
  val awsize  = WireDefault(io.awsize)

  /* Write data channel */
  val wready = WireDefault(isWriteIdle)
  val wdata  = WireDefault(io.wdata)
  val wstrb  = WireDefault(io.wstrb)
  Mem.io.ena   := isWriteIdle && awfire && wfire
  Mem.io.wea   := wstrb
  Mem.io.addra := (awaddr - area.start) >> 2.U
  Mem.io.dina  := wdata

  /* Write response channel */
  val bvalid = WireDefault(isSendBresp)
  val bresp  = WireDefault(TransactionResponse.okey.asUInt)
  bresp := TransactionResponse.okey.asUInt

  /* Read address channel */
  val arready       = WireDefault(isReadIdle)
  val arlen         = RegEnable(io.arlen + 1.U, isReadIdle)
  val araddr        = RegEnable(io.araddr, isReadIdle)
  val readCount     = RegInit(0.U(8.W))
  val nextReadCount = readCount + 1.U
  readCount    := Mux(io.rlast, 0.U, Mux(rfire, nextReadCount, readCount))
  Mem.io.enb   := isSendRresp
  Mem.io.addrb := ((araddr - area.start) >> 2.U) + readCount
  val readData = Mem.io.doutb

  /* Read data channel */
  // val delay       = LSFR(rfire)(2, 0)
  val delay      = 1.U(1.W)
  val delayCount = RegInit(0.U(delay.getWidth.W))
  val isFetch    = delayCount === delay - 1.U
  delayCount := MuxCase(
    delayCount,
    Seq(
      isReadIdle -> 0.U,
      (isSendRresp && !io.rvalid) -> (delayCount + 1.U)
    )
  )

  val rvalid = WireDefault(isFetch && isSendRresp)
  val rlast  = WireDefault(isSendRresp && isFetch && nextReadCount === arlen)
  val rdata  = WireDefault(readData)
  val rresp  = WireDefault(TransactionResponse.okey.asUInt)
  rresp := TransactionResponse.okey.asUInt

  /* io.bind */
  io.awready := awready
  io.wready  := wready
  io.bvalid  := bvalid
  io.bresp   := bresp
  io.bid     := DontCare
  io.arready := arready
  io.rvalid  := rvalid
  io.rdata   := rdata
  io.rresp   := rresp
  io.rlast   := rlast
  io.rid     := DontCare
}
