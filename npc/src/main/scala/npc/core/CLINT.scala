package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._

class CLINT(awidth: Int = 32, xlen: Int = 32, area: Area = Dev.CLINTAddr) extends Module {
  val io    = IO(Flipped(new AXI4MasterIO))
  val mtime = RegInit(0.U(64.W))
  mtime := mtime + 1.U

  val awfire = io.awvalid && io.awready
  val wfire  = io.wvalid && io.wready
  val bfire  = io.bvalid && io.bready
  val arfire = io.arvalid && io.arready
  val rfire  = io.rvalid && io.rready
  val rlast  = io.rlast

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
      sSendRresp -> Mux(rfire && rlast, sReadIdle, sSendRresp)
    )
  )
  /* Write address channel */
  val awready   = isWriteIdle
  val writeAddr = io.awaddr

  /* Write data channel */
  val wready    = isWriteIdle
  val writeData = io.wdata
  val writeMask = io.wstrb

  /* Write response channel */
  val bvalid = isSendBresp
  // val writeLow  = writeAddr === area.start
  // val writeHigh = writeAddr === (area.start + 4.U)
  // mtime := MuxCase(
  //   mtime + 1.U,
  //   Seq(
  //     (isWrite && writeLow) -> (mtime(63, 32) ## writeData),
  //     (isWrite && writeHigh) -> (writeData ## mtime(31, 0))
  //   )
  // )
  // val waddrValid = writeLow || writeHigh

  val bresp = TransactionResponse.slverr.asUInt

  /* Read address channel */
  val arready         = isReadIdle
  val readAddr        = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awidth - 1, 2) ## Fill(2, "b0".U)

  /* Read data channel */
  val rvalid   = isSendRresp
  val rdata    = Wire(UInt(xlen.W))
  val readLow  = alignedReadAddr === area.start
  val readHigh = alignedReadAddr === (area.start + 4.U)
  rdata := MuxCase(
    DontCare,
    Seq(
      (isSendRresp && readLow) -> mtime(31, 0),
      (isSendRresp && readHigh) -> mtime(63, 32)
    )
  )
  val raddrValid = readLow || readHigh
  val rresp      = Mux(raddrValid, TransactionResponse.okey.asUInt, TransactionResponse.decerr.asUInt)

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
