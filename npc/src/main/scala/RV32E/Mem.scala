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

class AXILiteMem(awith:Int = 32, dwith:Int = 32, size:Int = 4) extends RawModule {
  require(log2Ceil(size) < awith)
  require(log2Ceil(size) < dwith)

  val io = IO(new AXILiteIO(awith, dwith))
  val Mem = Module(new Mem)
withClockAndReset (io.aclk, !io.aresetn.asBool) {
  io.awid := DontCare // for manager is optional, so not realised
  io.bid  := DontCare // for manager is optional, so not realised
  io.rid  := DontCare // for manager is optional, so not realised

  io.awport := DontCare // for subordinate is optional, so not realised
  io.arport := DontCare // for subordinate is optional, so not realised

  val awfire = io.awvalid && io.awready
  val wfire  = io.wvalid && io.wready
  val bfire  = io.bvalid && io.bready
  val arfire = io.arvalid && io.arready
  val rfire  = io.rvalid && io.rready

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
  val alignedWriteAddr = writeAddr(awith-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val writeAddrAligned = writeAddr === alignedWriteAddr
  assert(writeAddrAligned)

  /* Write data channel */
  val wready      = RegInit(true.B)
  wready          := isIdle || isWaitWdata

  val writeData   = RegEnable(io.wdata, wfire)
  val writeMask   = RegEnable(io.wstarb, wfire)

  /* Write response channel */
  val bvalid      = RegEnable(Bool(), false.B, isWrite)
  val bresp       = RegEnable(UInt(2.W), "b00".U(2.W), isRead)
  bvalid          := Mux(bfire, false.B, true.B)
  bresp           := TransactionResponse.okey
  Mem.io.waddr    := Mux(awfire, io.awaddr, writeAddr)
  Mem.io.wdata    := Mux(wfire, io.wdata, writeData)
  Mem.io.wmask    := Mux(wfire, io.wstarb, writeMask)
  Mem.io.wen      := isWrite || (isWaitWaddr && awfire) || (isWaitWdata && wfire)

  /* Read address channel */
  val arready     = RegInit(true.B)

  arready         := isIdle
  val readAddr    = RegEnable(io.araddr, arfire)
  val alignedReadAddr = readAddr(awith-1, log2Ceil(size)) ## Fill(log2Ceil(size), "b0".U)
  val readAddrAligned = readAddr === alignedReadAddr
  assert(readAddrAligned)
  Mem.io.ren      := arfire
  Mem.io.raddr    := io.araddr
  val readData    = RegEnable(Mem.io.rdata, arfire)

  /* Read data channel */
  val rvalid      = RegEnable(Bool(), false.B, isRead)
  val rdata       = RegEnable(UInt(dwith.W), 0.U, isRead)
  val rresp       = RegEnable(UInt(2.W), "b00".U(2.W), isRead)

  val delay       = 5
  val delayCount  = RegInit(0.U(log2Ceil(delay).W))
  val isFetch     = delayCount === (delay - 1).U
  delayCount      := MuxCase(0.U, Seq(
    (isRead && !isFetch) -> (delayCount + 1.U),
    (isRead && isFetch) -> delayCount,
  ))
  rvalid          := Mux(rfire, false.B, Mux(rvalid, rvalid, isFetch))
  rdata           := readData
  rresp           := TransactionResponse.okey

}
}

