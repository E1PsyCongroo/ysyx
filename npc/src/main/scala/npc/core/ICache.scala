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
  val flush  = Input(Bool())
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
    val hit  = if (sim) Some(DecoupledIO(Output(Bool()))) else None
    val last = if (sim) Some(DecoupledIO(Output(Bool()))) else None
  })

  val cacheBlocksValid = Seq.fill(setNum, associativity)(RegInit(false.B))
  val cacheBlocksTag   = Seq.fill(setNum, associativity)(Reg(UInt(tagWidth.W)))
  val cacheBlocksData  = Seq.fill(setNum, associativity)(Reg(Vec(wordPerBlock, UInt(xlen.W))))

  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  val sIdle :: sCheck :: sSetAddr :: sFetch :: sSendBack :: Nil = Enum(5)

  val state      = RegInit(sIdle)
  val isIdle     = state === sIdle
  val isCheck    = state === sCheck
  val isSetAddr  = state === sSetAddr
  val isFetch    = state === sFetch
  val isSendBack = state === sSendBack

  val raddrReg       = WireDefault(io.in.bits.raddr)
  val offset         = if (offsetWidth != 0) raddrReg(offsetWidth + wordWidth - 1, wordWidth) else 0.U
  val set            = if (setWidth != 0) raddrReg(setWidth + blockWidth - 1, blockWidth) else 0.U
  val tag            = raddrReg(tagWidth + setWidth + blockWidth - 1, setWidth + blockWidth)
  val need           = WireDefault(needCache(raddrReg))
  val burst          = WireDefault(supportBurst(raddrReg))
  val cacheHit       = WireDefault(false.B)
  val cacheData      = Wire(UInt(xlen.W))
  val availableIndex = Reg(UInt(associativityWidth.W))

  val readCount     = RegInit(0.U((offsetWidth + 1).W))
  val nextReadCount = readCount + 1.U
  readCount := Mux(isIdle, 0.U, Mux(rfire, nextReadCount, readCount))
  val lastTranmit = (nextReadCount === wordPerBlock.U) || !need
  val rdataReg    = RegEnable(io.master.rdata, rfire)

  cacheData := DontCare
  for (setIndex <- 0 until setNum) {
    for (index <- 0 until associativity) {
      val tagHit = setIndex.U === set && cacheBlocksTag(setIndex)(index) === tag
      when(tagHit) {
        cacheHit  := cacheBlocksValid(setIndex)(index)
        cacheData := cacheBlocksData(setIndex)(index)(offset)
      }
      availableIndex := Mux(isCheck && setIndex.U === set && !cacheBlocksValid(setIndex)(index), index.U, 0.U)
      cacheBlocksValid(setIndex)(index) := Mux(
        io.flush,
        false.B,
        Mux(
          rfire && need && setIndex.U === set && index.U === availableIndex,
          true.B,
          cacheBlocksValid(setIndex)(index)
        )
      )
      cacheBlocksTag(setIndex)(index) := Mux(
        rfire && need && setIndex.U === set && index.U === availableIndex,
        tag,
        cacheBlocksTag(setIndex)(index)
      )
      cacheBlocksData(setIndex)(index)(readCount(offsetWidth - 1, 0)) := Mux(
        rfire && need && setIndex.U === set && index.U === availableIndex,
        io.master.rdata,
        cacheBlocksData(setIndex)(index)(readCount(offsetWidth - 1, 0))
      )
    }
  }

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
  io.master.awlen   := DontCare
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

    io.last.get.valid := isFetch && rfire && lastTranmit
    io.last.get.bits  := lastTranmit
  }
}
