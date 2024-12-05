package rvcpu.dev

import rvcpu.axi4._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class Uart(awidth: Int, dwidth: Int) extends Module {
  val size = dwidth / 8
  require(log2Ceil(size) < awidth)
  require(log2Ceil(size) < dwidth)

  val io = IO(Flipped(new AXI4MasterIO))

  val awfire = io.awvalid && io.awready
  val wfire  = io.wvalid && io.wready
  val bfire  = io.bvalid && io.bready
  val arfire = io.arvalid && io.arready
  val rfire  = io.rvalid && io.rready
  assert(!(arfire && (awfire || wfire)))

  val sIdle :: sWaitWaddr :: sWaitWdata :: sWrite :: sRead :: Nil = Enum(5)

  val state       = RegInit(sIdle)
  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitWdata = state === sWaitWdata
  val isWrite     = state === sWrite
  val isRead      = state === sRead

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          arfire -> sRead,
          (awfire && wfire) -> sWrite,
          wfire -> sWaitWaddr,
          awfire -> sWaitWdata
        )
      ),
      sWaitWaddr -> Mux(awfire, sWrite, sWaitWaddr),
      sWaitWdata -> Mux(wfire, sWrite, sWaitWdata),
      sWrite -> Mux(bfire, sIdle, sWrite),
      sRead -> Mux(rfire, sIdle, sRead)
    )
  )

  /* Write address channel */
  val awready          = WireDefault(isIdle || isWaitWaddr)
  val writeAddr        = RegEnable(io.awaddr, awfire)
  val alignedWriteAddr = writeAddr(awidth - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready    = WireDefault(isIdle || isWaitWdata)
  val writeData = RegEnable(io.wdata, wfire)
  val writeMask = RegEnable(io.wstrb, wfire)

  /* Write response channel */
  val bvalid     = WireDefault(isWrite)
  val writeValid = (writeMask === 1.U) && (writeAddr === 0.U)
  val bresp      = WireDefault(TransactionResponse.okey.asUInt)
  bresp := MuxCase(
    TransactionResponse.okey.asUInt,
    Seq(
      (isWrite && writeValid) -> TransactionResponse.okey.asUInt,
      (isWrite && !writeValid) -> TransactionResponse.decerr.asUInt
    )
  )
  when(isWrite && writeValid) {
    printf("%c", writeData(7, 0))
  }

  /* Read address channel */
  val arready         = WireDefault(isIdle)
  val readAddr        = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awidth - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)

  /* Read data channel */
  val rvalid = WireDefault(isRead)
  val rdata  = WireDefault(0.U(dwidth.W))
  val rresp  = WireDefault(TransactionResponse.okey.asUInt)

  rresp := TransactionResponse.slverr.asUInt

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
  io.rlast   := true.B
  io.rid     := DontCare
}
