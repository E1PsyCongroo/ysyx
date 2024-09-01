package rvcpu.dev

import rvcpu.utility._
import chisel3._
import chisel3.util._

import Dev.uartAddr

class Uart(awidth:Int = 32, dwidth:Int = 32, size:Int = 4) extends Module {
  require(log2Ceil(size) < awidth)
  require(log2Ceil(size) < dwidth)

  val io = IO(new AXILiteSubordinateIO(awidth, dwidth))

  val awid    = DontCare // for manager is optional, so not realised
  val bid     = DontCare // for manager is optional, so not realised
  val rid     = DontCare // for manager is optional, so not realised

  val awport  = DontCare // for subordinate is optional, so not realised
  val arport  = DontCare // for subordinate is optional, so not realised

  val awfire  = io.awvalid && io.awready
  val wfire   = io.wvalid && io.wready
  val bfire   = io.bvalid && io.bready
  val arfire  = io.arvalid && io.arready
  val rfire   = io.rvalid && io.rready
  assert(!(arfire && (awfire || wfire)))

  val sIdle :: sWaitWaddr :: sWaitWdata :: sWrite :: sRead :: Nil = Enum(5)
  val state = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(Seq(
    sIdle       -> MuxCase(sIdle, Seq(
      arfire -> sRead,

      (awfire && wfire) -> sWrite,
      wfire  -> sWaitWaddr,
      awfire -> sWaitWdata,
    )),
    sWaitWaddr  -> Mux(awfire, sWrite, sWaitWaddr),
    sWaitWdata  -> Mux(wfire, sWrite, sWaitWdata),
    sWrite      -> Mux(bfire, sIdle, sWrite),
    sRead       -> Mux(rfire, sIdle, sRead),
  ))

  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitWdata = state === sWaitWdata
  val isWrite     = state === sWrite
  val isRead      = state === sRead

  /* Write address channel */
  val awready     = WireDefault(true.B)
  awready         := isIdle || isWaitWaddr

  val writeAddr   = RegEnable(io.awaddr, awfire)
  val alignedWriteAddr = writeAddr(awidth-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready      = WireDefault(true.B)
  wready          := isIdle || isWaitWdata

  val writeData   = RegEnable(io.wdata, wfire)
  val writeMask   = RegEnable(io.wstarb, wfire)

  /* Write response channel */
  val bvalid      = WireDefault(false.B)
  bvalid          := Mux(isWrite, true.B, false.B)

  val writeValid  = (writeMask === 1.U) && (writeAddr >= uartAddr.start.U && writeAddr <= uartAddr.end.U)
  val bresp       = WireDefault(TransactionResponse.okey.asUInt)
  bresp           := MuxCase(TransactionResponse.okey.asUInt, Seq(
    (isWrite && writeValid)   -> TransactionResponse.okey.asUInt,
    (isWrite && !writeValid)  -> TransactionResponse.decerr.asUInt,
  ))
  when (isWrite && writeValid) {
    printf("%c", writeData(7, 0))
    SkipDifftest(clock, isWrite && writeValid)
  }

  /* Read address channel */
  val arready     = WireDefault(true.B)
  arready         := isIdle

  val readAddr    = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awidth-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)

  /* Read data channel */
  val rvalid      = WireDefault(false.B)
  rvalid          := Mux(isRead, true.B, false.B)

  val rdata       = WireDefault(0.U(dwidth.W))
  val rresp       = WireDefault(TransactionResponse.okey.asUInt)

  rresp           := TransactionResponse.slverr.asUInt

  /* IO bind */
  io.awready := awready
  io.wready  := wready
  io.bvalid  := bvalid
  io.bid     := bid
  io.bresp   := bresp
  io.arready := arready
  io.rvalid  := rvalid
  io.rid     := rid
  io.rdata   := rdata
  io.rresp   := rresp
}
