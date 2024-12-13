package rvcpu.dev

import rvcpu.axi4._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class Uart(params: AXI4BundleParameters) extends Module {
  require(params.dataBits == 32)
  require(params.lenPrenset)
  require(params.sizePresent)
  require(params.burstPresent)
  require(params.brespPresent)
  require(params.rlastPresent)
  require(params.rrespPresent)
  val size = params.dataBits / 8

  val io = IO(Flipped(AXI4Bundle(params)))

  val awfire = io.aw.fire
  val wfire  = io.w.fire
  val bfire  = io.b.fire
  val arfire = io.ar.fire
  val rfire  = io.r.fire
  assert(!arfire)

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
  val awid             = RegEnable(io.aw.bits.id, awfire)
  val writeAddr        = RegEnable(io.aw.bits.addr, awfire)
  val alignedWriteAddr = writeAddr(params.addrBits - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready    = WireDefault(isIdle || isWaitWdata)
  val writeData = RegEnable(io.w.bits.data, wfire)
  val writeMask = RegEnable(io.w.bits.strb, wfire)

  /* Write response channel */
  val bvalid     = WireDefault(isWrite)
  val writeValid = (writeMask === 1.U) && (writeAddr === 0.U)
  val bresp      = WireDefault(AXI4Parameters.RESP_OKAY)
  bresp := MuxCase(
    AXI4Parameters.RESP_OKAY,
    Seq(
      (isWrite && writeValid) -> AXI4Parameters.RESP_OKAY,
      (isWrite && !writeValid) -> AXI4Parameters.RESP_DECERR
    )
  )
  when(isWrite && writeValid) {
    printf("%c", writeData(7, 0))
  }

  /* Read address channel */
  val arready         = WireDefault(isIdle)
  val arid            = RegEnable(io.ar.bits.id, arfire)
  val readAddr        = RegEnable(io.ar.bits.addr, arfire)
  val alignedReadAddr = readAddr(params.addrBits - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)

  /* Read data channel */
  val rvalid = WireDefault(isRead)
  val rdata  = WireDefault(0.U(params.dataBits.W))
  val rresp  = WireDefault(AXI4Parameters.RESP_OKAY)

  /* io.bind */
  io.aw.ready    := awready
  io.w.ready     := wready
  io.b.valid     := bvalid
  io.b.bits      := DontCare
  io.b.bits.resp := bresp
  io.b.bits.id   := awid
  io.ar.ready    := arready
  io.r.valid     := rvalid
  io.r.bits      := DontCare
  io.r.bits.data := rdata
  io.r.bits.resp := rresp
  io.r.bits.last := true.B
  io.r.bits.id   := arid
}
