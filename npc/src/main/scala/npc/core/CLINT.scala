package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class CLINT(awidth: Int = 32, xlen: Int = 32, area: Area = Dev.CLINTAddr) extends Module {
  val io    = IO(Flipped(new AXI4MasterIO))
  val mtime = RegInit(0.U(64.W))

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
  val writeAddr        = RegEnable(io.awaddr, 0.U, awfire)
  val alignedWriteAddr = writeAddr(awidth - 1, 2) ## Fill(2, "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready    = WireDefault(isIdle || isWaitWdata)
  val writeData = RegEnable(io.wdata, wfire)
  val writeMask = RegEnable(io.wstrb, wfire)

  /* Write response channel */
  val bvalid    = WireDefault(isWrite)
  val writeLow  = writeAddrAligned === area.start
  val writeHigh = writeAddrAligned === (area.start + 4.U)
  mtime := MuxCase(
    mtime + 1.U,
    Seq(
      (isWrite && writeLow) -> (mtime(63, 32) ## writeData),
      (isWrite && writeHigh) -> (writeData ## mtime(31, 0))
    )
  )

  val waddrValid = writeLow || writeHigh
  val bresp = WireDefault(
    Mux(isWrite && !waddrValid, TransactionResponse.decerr.asUInt, TransactionResponse.okey.asUInt)
  )

  /* Read address channel */
  val arready         = WireDefault(isIdle)
  val readAddr        = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awidth - 1, 2) ## Fill(2, "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)

  /* Read data channel */
  val rvalid   = WireDefault(isRead)
  val rdata    = Wire(UInt(xlen.W))
  val readLow  = alignedReadAddr === area.start
  val readHigh = alignedReadAddr === (area.start + 4.U)
  rdata := MuxCase(
    DontCare,
    Seq(
      (isRead && readLow) -> mtime(31, 0),
      (isRead && readHigh) -> mtime(63, 32)
    )
  )
  val raddrValid = readLow || readHigh
  val rresp = WireDefault(
    Mux(isRead && !raddrValid, TransactionResponse.decerr.asUInt, TransactionResponse.okey.asUInt)
  )

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
