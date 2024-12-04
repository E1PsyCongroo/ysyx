package rvcpu.dev

import rvcpu.axi4._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.util.experimental.loadMemoryFromFileInline

class MemIO(awidth: Int, dwidth: Int) extends Bundle {
  val ren   = Input(Bool())
  val raddr = Input(UInt(awidth.W))
  val rdata = Output(UInt(dwidth.W))

  val wen   = Input(Bool())
  val waddr = Input(UInt(awidth.W))
  val wdata = Input(UInt(dwidth.W))
  val wmask = Input(UInt((dwidth / 8).W))
}

class DPIMem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO(32, 32) {
    val clock = Input(Clock())
  })
  addResource("/DPIMem.sv")
}

class InitMem(awidth: Int, dwidth: Int, program: String) extends Module {
  val size = dwidth / 8
  val io   = IO(new MemIO(awidth, dwidth))
  val mem  = Mem(BigInt("400000", 16), Vec(size, UInt(8.W)))
  loadMemoryFromFileInline(mem, program)
  val raddr = (io.raddr - "h8000_0000".U) >> 2.U
  io.rdata := Mux(io.ren, mem(raddr).asUInt, 0.U)
  val waddr = (io.waddr - "h8000_0000".U) >> 2.U
  val wdata = Wire(Vec(size, UInt(8.W)))
  for (i <- 0 until size) {
    wdata(i) := io.wdata(i * 8 + 7, i * 8)
  }
  val wmask = WireDefault(VecInit(io.wmask.asBools))
  when(io.wen) {
    mem.write(waddr, wdata, wmask)
  }
}

class AXI4Mem(awidth: Int, dwidth: Int, useProgram: Option[String] = None) extends Module {
  val size = dwidth / 8
  val io   = IO(Flipped(new AXI4MasterIO))

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
  val awready   = WireDefault(isWriteIdle)
  val writeAddr = WireDefault(io.awaddr)
  val writeSize = WireDefault(io.awsize)

  /* Write data channel */
  val wready    = WireDefault(isWriteIdle)
  val writeData = WireDefault(io.wdata)
  val writeMask = WireDefault(io.wstrb)

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
  readCount := Mux(io.rlast, 0.U, Mux(rfire, nextReadCount, readCount))
  val readData = if (useProgram.isEmpty) {
    val Mem = Module(new DPIMem)
    Mem.io.clock := clock
    Mem.io.waddr := writeAddr
    Mem.io.wdata := writeData
    Mem.io.wmask := writeMask
    Mem.io.wen   := isWriteIdle && awfire && wfire
    Mem.io.raddr := araddr(awidth - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U) + (readCount << 2.U)
    Mem.io.ren   := isSendRresp
    Mem.io.rdata
  } else {
    val Mem = Module(new InitMem(awidth, dwidth, useProgram.get))
    Mem.io.waddr := writeAddr
    Mem.io.wdata := writeData
    Mem.io.wmask := writeMask
    Mem.io.wen   := isWriteIdle && awfire && wfire
    Mem.io.raddr := araddr(awidth - 1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U) + (readCount << 2.U)
    Mem.io.ren   := isSendRresp
    Mem.io.rdata
  }

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
