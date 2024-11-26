package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._

import Dev.CLINTAddr

class ICacheOut(xlen: Int, sim: Boolean) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
  val fetchCycle  = if (sim) Some(Output(UInt(64.W))) else None
}

class ICacheTrace extends Bundle {
  val need        = Output(Bool())
  val hit         = Output(Bool())
  val accessStart = Output(Bool())
  val accessFin   = Output(Bool())
  val missStart   = Output(Bool())
  val missFin     = Output(Bool())
}

class ICacheIO(awidth: Int, xlen: Int, sim: Boolean) extends Bundle {
  val in     = Flipped(DecoupledIO(new IFUOut(awidth, sim)))
  val out    = DecoupledIO(new ICacheOut(xlen, sim))
  val trace  = if (sim) Some(new ICacheTrace) else None
  val flush  = Input(Bool())
  val clear  = Input(Bool())
  val master = new AXI4MasterIO
}

class ICache(
  awidth:             Int     = 32,
  xlen:               Int     = 32,
  totalWidth:         Int     = 4,
  blockWidth:         Int     = 2,
  associativityWidth: Int     = 0,
  needCache:          UInt => Bool,
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

  val io = IO(new ICacheIO(awidth, xlen, sim) {
    val curCycle = if (sim) Some(Input(UInt(64.W))) else None
  })

  val cacheBlocksValid = Seq.fill(setNum, associativity)(RegInit(false.B))
  val cacheBlocksTag   = Seq.fill(setNum, associativity)(Reg(UInt(tagWidth.W)))
  val cacheBlocksData  = Seq.fill(setNum, associativity)(Reg(Vec(wordPerBlock, UInt(xlen.W))))

  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready
  val rlast  = io.master.rlast

  val sCheck :: sSetAddr :: sWaitResp :: sSendBack :: Nil = Enum(4)

  val state        = RegInit(sCheck)
  val isCheck      = state === sCheck
  val isSetAddr    = state === sSetAddr
  val isWaitResp   = state === sWaitResp
  val isSendBack   = state === sSendBack

  val skip = RegInit(false.B)
  skip := MuxCase(
    skip,
    Seq(
      isSendBack -> false.B,
      io.flush -> true.B
    )
  )

  val raddr          = WireDefault(io.in.bits.pc)
  val offset         = if (offsetWidth != 0) raddr(offsetWidth + wordWidth - 1, wordWidth) else 0.U
  val set            = if (setWidth != 0) raddr(setWidth + blockWidth - 1, blockWidth) else 0.U
  val tag            = raddr(tagWidth + setWidth + blockWidth - 1, setWidth + blockWidth)
  val need           = WireDefault(needCache(raddr))
  val cacheHit       = WireDefault(false.B)
  val cacheData      = Wire(UInt(xlen.W))
  val availableIndex = Reg(UInt(associativityWidth.W))
  val rdataReg       = RegEnable(io.master.rdata, rfire && rlast)

  val readCount     = RegInit(0.U((offsetWidth + 1).W))
  val nextReadCount = readCount + 1.U
  readCount := Mux(isCheck, 0.U, Mux(rfire, nextReadCount, readCount))

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
        io.clear,
        false.B,
        Mux(
          rfire && rlast && need && setIndex.U === set && index.U === availableIndex,
          true.B,
          cacheBlocksValid(setIndex)(index)
        )
      )
      cacheBlocksTag(setIndex)(index) := Mux(
        rfire && rlast && need && setIndex.U === set && index.U === availableIndex,
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

  state := MuxLookup(state, sCheck)(
    Seq(
      sCheck -> Mux(io.in.valid, Mux(cacheHit, sSendBack, sSetAddr), sCheck),
      sSetAddr -> Mux(arfire, sWaitResp, sSetAddr),
      sWaitResp -> Mux(rfire && rlast, sSendBack, sWaitResp),
      sSendBack -> Mux(io.out.fire || skip, sCheck, sSendBack)
    )
  )

  assert(!rfire || io.master.rresp === "b00".U(2.W))
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
  io.master.araddr  := Mux(need, raddr(xlen - 1, blockWidth) ## Fill(blockWidth, "b0".U), raddr)

  io.master.arid    := 0.U
  io.master.arlen   := Mux(need, (wordPerBlock - 1).U, 0.U)
  io.master.arsize  := "b010".U
  io.master.arburst := "b01".U

  io.master.rready := isWaitResp

  io.in.ready := isCheck && !io.in.valid

  io.out.valid            := isSendBack && !skip
  io.out.bits.pc          := raddr
  io.out.bits.instruction := Mux(need, cacheData, rdataReg)

  if (sim) {
    io.out.bits.fetchCycle.get := io.curCycle.get

    io.trace.get.hit         := RegEnable(cacheHit, isCheck)
    io.trace.get.need        := RegEnable(need, isCheck)
    io.trace.get.accessStart := isCheck && io.in.valid
    io.trace.get.accessFin   := io.out.valid
    io.trace.get.missStart   := io.master.arvalid
    io.trace.get.missFin     := rfire && rlast
  }
}
