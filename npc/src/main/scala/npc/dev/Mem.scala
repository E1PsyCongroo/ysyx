package rvcpu.dev

import rvcpu.axi4._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class MemIO extends Bundle {
  val ren   = Input(Bool())
  val raddr = Input(UInt(32.W))
  val rdata = Output(UInt(32.W))

  val wen   = Input(Bool())
  val waddr = Input(UInt(32.W))
  val wdata = Input(UInt(32.W))
  val wmask = Input(UInt(4.W))
}

class Mem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO {
    val clock = Input(Clock())
  })
  addResource("/Mem.sv")
}

class AXI4Mem(awidth: Int = 32, dwidth: Int = 32, size: Int = 4) extends Module {
  require(log2Ceil(size) < awidth)
  require(log2Ceil(size) < dwidth)

  val io  = IO(Flipped(new AXI4MasterIO))
  val Mem = Module(new Mem)
  Mem.io.clock := clock

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
  val writeSize        = RegEnable(io.awsize, awfire)

  /* Write data channel */
  val wready    = WireDefault(isIdle || isWaitWdata)
  val writeData = RegEnable(io.wdata, wfire)
  val writeMask = RegEnable(io.wstrb, wfire)

  /* Write response channel */
  val bvalid = WireDefault(isWrite)
  val bresp  = WireDefault(TransactionResponse.okey.asUInt)
  bresp        := TransactionResponse.okey.asUInt
  Mem.io.waddr := writeAddr
  Mem.io.wdata := writeData
  Mem.io.wmask := writeMask
  Mem.io.wen   := isWrite

  /* Read address channel */
  val arready = WireDefault(isIdle)
  Mem.io.raddr := io.araddr
  Mem.io.ren   := arfire
  val readData = RegEnable(Mem.io.rdata, arfire)

  /* Read data channel */
  // val delay       = LSFR(rfire)
  val delay      = 1.U(1.W)
  val delayCount = RegInit(0.U(delay.getWidth.W))
  val isFetch    = delayCount === delay - 1.U
  delayCount := MuxCase(
    0.U,
    Seq(
      (isRead && !isFetch) -> (delayCount + 1.U),
      (isRead && isFetch) -> delayCount
    )
  )

  val rvalid = WireDefault(Mux(isRead, isFetch, false.B))
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
  io.rlast   := true.B
  io.rid     := DontCare
}
