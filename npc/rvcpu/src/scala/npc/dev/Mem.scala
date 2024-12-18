package rvcpu.dev

import rvcpu.axi4._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import os.read

class MemIO(awidth: Int, dwidth: Int) extends Bundle {
  val ren   = Input(Bool())
  val raddr = Input(UInt(awidth.W))
  val rdata = Output(UInt(dwidth.W))

  val wen   = Input(Bool())
  val waddr = Input(UInt(awidth.W))
  val wdata = Input(UInt(dwidth.W))
  val wmask = Input(UInt((dwidth / 8).W))
}

abstract class AbstractMem(awidth: Int, dwidth: Int) extends Module {
  val io = IO(new MemIO(awidth, dwidth))
}

class DPIMem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO(32, 32) {
    val clock = Input(Clock())
    val reset = Input(Reset())
  })
  addResource("/DPIMem.sv")
}

class DPIMemWrapper extends AbstractMem(32, 32) {
  val mem = Module(new DPIMem)
  mem.io.clock := clock
  mem.io.reset := reset
  mem.io.ren   := io.ren
  mem.io.raddr := io.raddr
  io.rdata     := mem.io.rdata

  mem.io.wen   := io.wen
  mem.io.waddr := io.waddr
  mem.io.wdata := io.wdata
  mem.io.wmask := io.wmask
}

class InitMem(awidth: Int, dwidth: Int) extends AbstractMem(awidth, dwidth) {
  val size = dwidth / 8
  val mem  = Mem(BigInt("400000", 16), Vec(size, UInt(8.W)))

  val raddr = io.raddr
  io.rdata := Mux(io.ren, mem(raddr).asUInt, 0.U)

  val waddr = io.waddr
  val wdata = Wire(Vec(size, UInt(8.W)))
  for (i <- 0 until size) {
    wdata(i) := io.wdata(i * 8 + 7, i * 8)
  }
  val wmask = WireDefault(VecInit(io.wmask.asBools))
  when(io.wen) {
    mem.write(waddr, wdata, wmask)
  }
}

class AXI4Mem(params: AXI4BundleParameters, useDPIC: Boolean) extends Module {
  require(params.lenPrenset)
  require(params.sizePresent)
  require(params.burstPresent)
  require(params.brespPresent)
  require(params.rlastPresent)
  require(params.rrespPresent)
  val size = params.dataBits / 8
  val io   = IO(Flipped(AXI4Bundle(params)))
  val Mem  = Module(if (useDPIC) (new DPIMemWrapper) else (new InitMem(params.addrBits, params.dataBits)))

  val awfire = io.aw.fire
  val wfire  = io.w.fire
  val bfire  = io.b.fire
  val arfire = io.ar.fire
  val rfire  = io.r.fire
  val rlast  = io.r.bits.last(0)

  val sWriteIdle :: sWaitAW :: sWaitW :: sSendBresp :: Nil = Enum(4)
  val sReadIdle :: sSendRresp :: Nil                       = Enum(2)

  val writeState  = RegInit(sWriteIdle)
  val isWriteIdle = writeState === sWriteIdle
  val isWaitAW    = writeState === sWaitAW
  val isWaitW     = writeState === sWaitW
  val isSendBresp = writeState === sSendBresp

  val readState   = RegInit(sReadIdle)
  val isReadIdle  = readState === sReadIdle
  val isSendRresp = readState === sSendRresp

  writeState := MuxLookup(writeState, sWriteIdle)(
    Seq(
      sWriteIdle -> MuxCase(
        sWriteIdle,
        Seq(
          (awfire && wfire) -> sSendBresp,
          (wfire) -> sWaitAW,
          (awfire) -> sWaitW
        )
      ),
      sWaitAW -> Mux(awfire, sSendBresp, sWaitAW),
      sWaitW -> Mux(wfire, sSendBresp, sWaitW),
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
  val awready = WireDefault(isWriteIdle || isWaitAW)
  val awid    = RegEnable(io.aw.bits.id, awfire)
  val awaddr  = RegEnable(io.aw.bits.addr, awfire)
  val awsize  = RegEnable(io.aw.bits.size, awfire)

  /* Write data channel */
  val wready = WireDefault(isWriteIdle || isWaitW)
  val wdata  = RegEnable(io.w.bits.data, wfire)
  val wstrb  = RegEnable(io.w.bits.strb, wfire)

  /* Write response channel */
  val bvalid = WireDefault(isSendBresp)
  val bresp  = WireDefault(AXI4Parameters.RESP_OKAY)

  /* Read address channel */
  val arready       = WireDefault(isReadIdle)
  val arid          = RegEnable(io.ar.bits.id, isReadIdle)
  val arlen         = RegEnable(io.ar.bits.len + 1.U, isReadIdle)
  val araddr        = RegEnable(io.ar.bits.addr, isReadIdle)
  val readCount     = RegInit(0.U(8.W))
  val nextReadCount = readCount + 1.U
  readCount := Mux(rfire, Mux(rlast, 0.U, nextReadCount), readCount)
  val readData = Mem.io.rdata

  /* Read data channel */
  val delay       = LSFR(rfire)(1, 0)
  val delayCount = Reg(UInt(delay.getWidth.W))
  val isFetch    = delayCount === delay - 1.U
  delayCount := MuxLookup(readState, delayCount)(
    Seq(
      sReadIdle -> 0.U,
      sSendRresp -> Mux(rfire, 0.U, Mux(isFetch, delayCount, delayCount + 1.U))
    )
  )

  val rvalid   = WireDefault(isFetch && isSendRresp)
  val lastRead = WireDefault(nextReadCount === arlen)
  val rdata    = WireDefault(readData)
  val rresp    = WireDefault(AXI4Parameters.RESP_OKAY)

  Mem.io.waddr := awaddr >> 2.U
  Mem.io.wdata := wdata
  Mem.io.wmask := wstrb
  Mem.io.wen   := bfire
  Mem.io.raddr := (araddr >> 2.U) + readCount
  Mem.io.ren   := isSendRresp && isFetch

  /* io.bind */
  io.aw.ready    := awready
  io.w.ready     := wready
  io.b.valid     := bvalid
  io.b.bits      := DontCare
  io.b.bits.id   := awid
  io.b.bits.resp := bresp
  io.ar.ready    := arready
  io.r.valid     := rvalid
  io.r.bits      := DontCare
  io.r.bits.id   := arid
  io.r.bits.data := rdata
  io.r.bits.resp := rresp
  io.r.bits.last := lastRead
}
