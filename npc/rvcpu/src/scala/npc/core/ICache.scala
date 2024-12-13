package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import scala.collection.immutable._

class ICacheOut(cpuConfig: CPUConfig) extends Bundle {
  val pc          = UInt(cpuConfig.xlen.W)
  val instruction = UInt(32.W)
  val state       = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class ICacheIO(cpuConfig: CPUConfig, params: AXI4BundleParameters) extends Bundle {
  require(params.addrBits == cpuConfig.xlen)
  require(params.dataBits == cpuConfig.xlen)
  require(params.lenPrenset)
  require(params.sizePresent)
  require(params.burstPresent)
  require(params.rlastPresent)
  require(params.rrespPresent)
  val in     = Flipped(DecoupledIO(new IFUOut(cpuConfig)))
  val out    = DecoupledIO(new ICacheOut(cpuConfig))
  val clear  = Input(Bool())
  val master = AXI4Bundle(params)

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
}

class ICache(cpuConfig: CPUConfig, params: AXI4BundleParameters) extends Module {
  val wordWidth          = log2Ceil(cpuConfig.xlen / 8)
  val totalWidth         = cpuConfig.icacheConfig.totalWidth
  val blockWidth         = cpuConfig.icacheConfig.blockWidth
  val associativityWidth = cpuConfig.icacheConfig.associativityWidth
  val icacheNeed         = cpuConfig.icacheConfig.icacheNeed
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
  val tagWidth      = cpuConfig.xlen - setWidth - blockWidth

  val io = IO(new ICacheIO(cpuConfig, params))

  val cacheBlocksValid = Seq.fill(setNum, associativity)(RegInit(false.B))
  val cacheBlocksTag   = Seq.fill(setNum, associativity)(Reg(UInt(tagWidth.W)))
  val cacheBlocksData  = Seq.fill(setNum, associativity)(Reg(Vec(wordPerBlock, UInt(cpuConfig.xlen.W))))

  val arfire = io.master.ar.fire
  val rfire  = io.master.r.fire
  val rlast  = io.master.r.bits.last
  val rdata  = io.master.r.bits.data

  val sCheck :: sSetAddr :: sWaitResp :: Nil = Enum(3)

  val state      = RegInit(sCheck)
  val nextState  = WireDefault(sCheck)
  val isCheck    = state === sCheck
  val isSetAddr  = state === sSetAddr
  val isWaitResp = state === sWaitResp

  val raddr  = WireDefault(io.in.bits.pc)
  val offset = if (offsetWidth != 0) raddr(offsetWidth + wordWidth - 1, wordWidth) else 0.U
  val set    = if (setWidth != 0) raddr(setWidth + blockWidth - 1, blockWidth) else 0.U
  val tag    = raddr(tagWidth + setWidth + blockWidth - 1, setWidth + blockWidth)
  val need   = WireDefault(icacheNeed.contains(raddr))
  val cacheSelSet =
    for (i <- 0 until associativity)
      yield {
        val tags   = Mux1H((0 until setNum).map { j => (set === j.U, cacheBlocksTag(j)(i)) })
        val valids = Mux1H((0 until setNum).map { j => (set === j.U, cacheBlocksValid(j)(i)) })
        val datas  = Mux1H((0 until setNum).map { j => (set === j.U, cacheBlocksData(j)(i)) })
        (tags, valids, datas)
      }
  val cacheHit = WireDefault(MuxLookup(tag, false.B)((0 until associativity).map { i =>
    (cacheSelSet(i)._1, cacheSelSet(i)._2)
  }))
  val cacheData = WireDefault(Mux1H((0 until associativity).map { i =>
    (tag === cacheSelSet(i)._1, cacheSelSet(i)._3(offset))
  }))
  val randChoose = if (associativityWidth == 0) 0.U else LSFR(true.B)(associativityWidth - 1, 0)
  val availableIndex =
    RegEnable(MuxCase(randChoose, (0 until associativity).map { i => (!cacheSelSet(i)._2, i.U) }), isCheck)
  val readCount     = RegInit(0.U((offsetWidth + 1).W))
  val nextReadCount = readCount + 1.U
  readCount := MuxCase(
    readCount,
    Seq(
      isCheck -> 0.U,
      rfire -> nextReadCount
    )
  )

  for (setIndex <- 0 until setNum) {
    for (index <- 0 until associativity) {
      cacheBlocksTag(setIndex)(index) := Mux(
        rfire && rlast(0) && need && setIndex.U === set && index.U === availableIndex,
        tag,
        cacheBlocksTag(setIndex)(index)
      )
      cacheBlocksValid(setIndex)(index) := Mux(
        io.clear,
        false.B,
        Mux(
          rfire && rlast(0) && need && setIndex.U === set && index.U === availableIndex,
          true.B,
          cacheBlocksValid(setIndex)(index)
        )
      )
      cacheBlocksData(setIndex)(index)(readCount(offsetWidth - 1, 0)) := Mux(
        rfire && need && setIndex.U === set && index.U === availableIndex,
        rdata,
        cacheBlocksData(setIndex)(index)(readCount(offsetWidth - 1, 0))
      )
    }
  }

  val rdataValid = RegInit(false.B)
  rdataValid := Mux(io.out.fire, false.B, Mux(rfire && rlast(0), true.B, rdataValid))
  val rdataReg  = RegEnable(rdata, rfire && rlast(0))
  val dataValid = Mux(need, cacheHit, rdataValid)

  nextState := MuxLookup(state, sCheck)(
    Seq(
      sCheck -> Mux(io.in.valid && !dataValid, sSetAddr, sCheck),
      sSetAddr -> Mux(arfire, sWaitResp, sSetAddr),
      sWaitResp -> Mux(rfire && rlast(0), sCheck, sWaitResp)
    )
  )
  state := nextState

  /* IO bind */
  io.master.aw.valid := false.B
  io.master.aw.bits  := DontCare

  io.master.w.valid := false.B
  io.master.w.bits  := DontCare

  io.master.b.ready := false.B

  io.master.ar.valid      := isSetAddr
  io.master.ar.bits       := DontCare
  io.master.ar.bits.addr  := Mux(need, raddr(cpuConfig.xlen - 1, blockWidth) ## Fill(blockWidth, "b0".U), raddr)
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := Mux(need, (wordPerBlock - 1).U, 0.U)
  io.master.ar.bits.size  := "b010".U
  io.master.ar.bits.burst := AXI4Parameters.BURST_INCR

  io.master.r.ready := isWaitResp

  io.in.ready := (isCheck && !io.in.valid) || io.out.fire

  io.out.valid            := io.in.valid && dataValid
  io.out.bits.pc          := raddr
  io.out.bits.instruction := Mux(need, cacheData, rdataReg)

  if (cpuConfig.sim) {
    assert(!rfire || io.master.r.bits.resp === AXI4Parameters.RESP_OKAY)

    val icacheFetchStart  = RegEnable(io.curCycle.get, nextState === sSetAddr)
    val icacheFetchFinish = RegEnable(io.curCycle.get, nextState === sCheck)
    val hasAXIReq         = RegInit(false.B)
    hasAXIReq := (hasAXIReq || arfire) && io.in.valid && !io.out.fire

    io.out.bits.state.get                := io.in.bits.state.get
    io.out.bits.state.get.icacheCycle    := io.curCycle.get
    io.out.bits.state.get.inst           := io.out.bits.instruction
    io.out.bits.state.get.icacheNeed     := need
    io.out.bits.state.get.icacheHit      := !hasAXIReq
    io.out.bits.state.get.icacheMissTime := Mux(need && hasAXIReq, icacheFetchFinish - icacheFetchStart, 0.U)
  }
}
