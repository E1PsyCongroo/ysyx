package RVCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class MemIO extends Bundle {
  val ren     = Input(Bool())
  val raddr   = Input(UInt(32.W))
  val rdata   = Output(UInt(32.W))

  val wen     = Input(Bool())
  val waddr   = Input(UInt(32.W))
  val wdata   = Input(UInt(32.W))
  val wmask   = Input(UInt(4.W))
}

class Mem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO{
    val clock = Input(Clock())
  })
  addResource("/Mem.sv")
}

class AXILiteMem(awidth:Int = 32, dwidth:Int = 32, size:Int = 4) extends Module {
  require(log2Ceil(size) < awidth)
  require(log2Ceil(size) < dwidth)

  val io = IO(new AXILiteIO(awidth, dwidth))
  val Mem = Module(new Mem)
  Mem.io.clock := clock
withReset (!reset.asBool) {
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

  val isIdle  = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitWdata = state === sWaitWdata
  val isWrite = state === sWrite
  val isRead  = state === sRead

  /* Write address channel */
  val awready     = RegInit(true.B)
  awready         := isIdle || isWaitWaddr

  val writeAddr   = RegEnable(io.awaddr, awfire)
  val alignedWriteAddr = writeAddr(awidth-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready      = RegInit(true.B)
  wready          := isIdle || isWaitWdata

  val writeData   = RegEnable(io.wdata, wfire)
  val writeMask   = RegEnable(io.wstarb, wfire)

  /* Write response channel */
  val bvalid      = RegInit(false.B)
  bvalid          := Mux(isWrite, Mux(bfire, false.B, true.B), bvalid)

  val bresp       = RegInit(TransactionResponse.okey.asUInt)
  bresp           := Mux(isWrite, TransactionResponse.okey.asUInt, bresp)
  Mem.io.waddr    := Mux(awfire, io.awaddr, writeAddr)
  Mem.io.wdata    := Mux(wfire, io.wdata, writeData)
  Mem.io.wmask    := Mux(wfire, io.wstarb, writeMask)
  Mem.io.wen      := isWrite || (isWaitWaddr && awfire) || (isWaitWdata && wfire)

  /* Read address channel */
  val arready     = RegInit(true.B)
  arready         := isIdle

  val readAddr    = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awidth-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)
  Mem.io.ren      := arfire
  Mem.io.raddr    := io.araddr
  val readData    = RegEnable(Mem.io.rdata, arfire)

  /* Read data channel */
  val rvalid      = RegInit(false.B)

  val rdata       = RegEnable(readData, 0.U, isRead)
  val rresp       = RegInit(TransactionResponse.okey.asUInt)

  val delay       = 5
  val delayCount  = RegInit(0.U(log2Ceil(delay).W))
  val isFetch     = delayCount === (delay - 1).U
  delayCount      := MuxCase(0.U, Seq(
    (isRead && !isFetch) -> (delayCount + 1.U),
    (isRead && isFetch) -> delayCount,
  ))
  rvalid          := Mux(isRead, Mux(rfire, false.B, Mux(rvalid, rvalid, isFetch)), rvalid)
  rresp           := Mux(isRead && isFetch, TransactionResponse.okey.asUInt, rresp)

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
}

