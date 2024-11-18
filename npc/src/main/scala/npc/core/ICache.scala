package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._

import Dev.CLINTAddr

class ICacheIn(awidth: Int = 32) extends Bundle {
  val raddr = Input(UInt(awidth.W))
}

class ICacheOut(xlen: Int = 32) extends Bundle {
  val rdata = Output(UInt(xlen.W))
}

class ICacheIO(awidth: Int = 32, xlen: Int = 32) extends Bundle {
  val in     = Flipped(DecoupledIO(new ICacheIn))
  val out    = DecoupledIO(new ICacheOut)
  val master = new AXI4MasterIO
}

class ICache(
  awidth:             Int     = 32,
  xlen:               Int     = 32,
  totalWidth:         Int     = 4,
  blockWidth:         Int     = 2,
  associativityWidth: Int     = 0,
  needCache:          UInt => Bool,
  supportBurst:       UInt => Bool,
  sim:                Boolean = true)
    extends Module {
  val wordWidth = log2Ceil(xlen / 8)
  require(blockWidth >= wordWidth)
  require(totalWidth >= blockWidth)
  require(associativityWidth >= 0)
  require((totalWidth - blockWidth) >= associativityWidth)
  val blockNum      = 1 << (totalWidth - blockWidth)
  val associativity = 1 << associativityWidth
  val setNum        = blockNum / associativity
  val offsetWidth   = blockWidth - wordWidth
  val wordPerBlock  = 1 << offsetWidth
  val setWidth      = log2Ceil(setNum)
  val tagWidth      = awidth - setWidth - blockWidth

  val io = IO(new ICacheIO(awidth, xlen) {
    val hit = if (sim) Some(DecoupledIO(Output(Bool()))) else None
  })

  val cacheLinesValid = RegInit(VecInit.fill(setNum, associativity)(false.B))
  val cacheLinesTag   = Reg(Vec(setNum, Vec(associativity, UInt(tagWidth.W))))
  val cacheLinesData  = Reg(Vec(setNum, Vec(associativity, Vec(wordPerBlock, UInt(xlen.W)))))

  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  val sIdle :: sCheck :: sSetAddr :: sFetch :: sSendBack :: Nil = Enum(5)

  val state      = RegInit(sIdle)
  val isIdle     = state === sIdle
  val isCheck    = state === sCheck
  val isSetAddr  = state === sSetAddr
  val isFetch    = state === sFetch
  val isSendBack = state === sSendBack

  val raddrReg = RegEnable(io.in.bits.raddr, io.in.fire)
  val offset   = if (offsetWidth != 0) raddrReg(offsetWidth + wordWidth - 1, wordWidth) else 0.U
  val set      = if (setWidth != 0) raddrReg(setWidth + blockWidth - 1, blockWidth) else 0.U
  val tag      = raddrReg(tagWidth + setWidth + blockWidth - 1, setWidth + blockWidth)

  val need       = WireDefault(needCache(raddrReg))
  val burst      = WireDefault(supportBurst(raddrReg))
  val indexInSet = WireDefault(cacheLinesTag(set).onlyIndexWhere(_ === tag))
  val cacheHit   = WireDefault(cacheLinesTag(set).contains(tag) && cacheLinesValid(set)(indexInSet))
  val cacheData  = WireDefault(cacheLinesData(set)(indexInSet)(offset))

  val readCount     = if (offsetWidth != 0) RegInit(0.U((offsetWidth + 1).W)) else 0.U
  val nextReadCount = readCount + 1.U
  if (offsetWidth != 0) {
    readCount := MuxCase(
      readCount,
      Seq(
        isIdle -> 0.U,
        rfire -> nextReadCount
      )
    )
  }
  val lastTranmit = (nextReadCount === wordPerBlock.U) || !need

  val availableIndex = Reg(UInt(associativityWidth.W))
  availableIndex                       := Mux(isCheck, cacheLinesValid(set).indexWhere(_ === false.B), availableIndex)
  cacheLinesValid(set)(availableIndex) := Mux(rfire && need, true.B, cacheLinesValid(set)(availableIndex))
  cacheLinesTag(set)(availableIndex)   := Mux(rfire && need, tag, cacheLinesTag(set)(availableIndex))
  cacheLinesData(set)(availableIndex)(readCount(offsetWidth - 1, 0)) := Mux(
    rfire && need,
    io.master.rdata,
    cacheLinesData(set)(availableIndex)(readCount(offsetWidth - 1, 0))
  )
  val rdataReg = RegEnable(io.master.rdata, rfire)

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sCheck, sIdle),
      sCheck -> Mux(need && cacheHit, sSendBack, sSetAddr),
      sSetAddr -> Mux(arfire, sFetch, sSetAddr),
      sFetch -> MuxCase(
        sFetch,
        Seq(
          (rfire && !burst && !lastTranmit) -> sSetAddr,
          (rfire && lastTranmit) -> sSendBack
        )
      ),
      sSendBack -> Mux(io.out.fire, sIdle, sSendBack)
    )
  )

  assert(!rfire || io.master.rresp === "b00".U(2.W))
  assert(!(rfire && lastTranmit) || io.master.rlast)
  /* IO bind */
  io.master.awvalid := false.B
  io.master.awaddr  := DontCare
  io.master.awid    := DontCare
  io.master.awlen   := 0.U
  io.master.awsize  := DontCare
  io.master.awburst := DontCare

  io.master.wvalid := false.B
  io.master.wdata  := DontCare
  io.master.wstrb  := DontCare
  io.master.wlast  := DontCare

  io.master.bready := false.B

  io.master.arvalid := isSetAddr
  io.master.araddr := Mux(
    need,
    raddrReg(xlen - 1, blockWidth) ## Fill(blockWidth, "b0".U) + (readCount << 2.U),
    raddrReg
  )
  io.master.arid    := 0.U
  io.master.arlen   := Mux(need && burst, (wordPerBlock - 1).U, 0.U)
  io.master.arsize  := "b010".U
  io.master.arburst := "b01".U

  io.master.rready := isFetch

  io.in.ready := isIdle

  io.out.valid      := isSendBack
  io.out.bits.rdata := Mux(need, cacheData, rdataReg)

  if (sim) {
    io.hit.get.valid := isCheck
    io.hit.get.bits  := cacheHit
  }
}
