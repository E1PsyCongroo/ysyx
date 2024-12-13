package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import rvcpu.NPCConfig.axiParams

class CLINT(cpuConfig: CPUConfig, params: AXI4BundleParameters) extends Module {
  require(cpuConfig.frequency % 1000_000 == 0)
  require(params.wstrbPresent)
  require(params.rlastPresent)
  require(params.brespPresent)
  require(params.rrespPresent)
  require(params.addrBits > 2)
  require(params.dataBits == cpuConfig.xlen)
  val cycle_per_us = cpuConfig.frequency / 1000_000
  val io           = IO(Flipped(new AXI4Bundle(params)))
  val mtime        = RegInit(0.U(64.W))
  val usCounter    = RegInit(0.U(log2Ceil(cycle_per_us).W))
  val us           = usCounter === (cycle_per_us - 1).U
  usCounter := Mux(us, 0.U, usCounter + 1.U)
  mtime     := Mux(us, mtime + 1.U, mtime)

  val awfire = io.aw.fire
  val wfire  = io.w.fire
  val bfire  = io.b.fire
  val arfire = io.ar.fire
  val rfire  = io.r.fire && io.r.bits.last(0)

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
      sSendRresp -> Mux(rfire, sReadIdle, sSendRresp)
    )
  )
  /* Write address channel */
  val awready   = isWriteIdle && io.w.valid
  val awid      = RegEnable(io.aw.bits.id, isWriteIdle)
  val writeAddr = io.aw.bits.addr

  /* Write data channel */
  val wready    = isWriteIdle && io.aw.valid
  val writeData = io.w.bits.data
  val writeMask = io.w.bits.strb

  /* Write response channel */
  val bvalid = isSendBresp
  // val writeLow  = writeAddr === 0.U
  // val writeHigh = writeAddr === 4.U
  // mtime := MuxCase(
  //   mtime + 1.U,
  //   Seq(
  //     (isWrite && writeLow) -> (mtime(63, 32) ## writeData),
  //     (isWrite && writeHigh) -> (writeData ## mtime(31, 0))
  //   )
  // )
  // val waddrValid = writeLow || writeHigh

  val bresp = AXI4Parameters.RESP_SLVERR

  /* Read address channel */
  val arready         = isReadIdle
  val arid            = RegEnable(io.ar.bits.id, isReadIdle)
  val readAddr        = io.ar.bits.addr
  val alignedReadAddr = readAddr(params.addrBits - 1, 2) ## Fill(2, "b0".U)

  /* Read data channel */
  val rvalid   = isSendRresp
  val rlast    = isSendRresp
  val rdata    = Wire(UInt(axiParams.dataBits.W))
  val readLow  = RegNext(alignedReadAddr === 0.U)
  val readHigh = RegNext(alignedReadAddr === 4.U)
  rdata := MuxCase(
    DontCare,
    Seq(
      (isSendRresp && readLow) -> mtime(31, 0),
      (isSendRresp && readHigh) -> mtime(63, 32)
    )
  )
  val raddrValid = readLow || readHigh
  val rresp      = Mux(raddrValid, AXI4Parameters.RESP_OKAY, AXI4Parameters.RESP_DECERR)

  /* io.bind */
  io.aw.ready    := awready
  io.w.ready     := wready
  io.b.valid     := bvalid
  io.b.bits.resp := bresp
  io.b.bits.id   := awid
  io.ar.ready    := arready
  io.r.valid     := rvalid
  io.r.bits.data := rdata
  io.r.bits.resp := rresp
  io.r.bits.last := rlast
  io.r.bits.id   := arid
}
