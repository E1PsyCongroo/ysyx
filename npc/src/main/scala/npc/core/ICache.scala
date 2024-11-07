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
  awidth:    Int     = 32,
  xlen:      Int     = 32,
  lineSize:  Int     = 4,
  lineNum:   Int,
  wayNum:    Int,
  needCache: UInt => Bool,
  sim:       Boolean = true)
    extends Module {
  require(lineSize * 8 >= xlen)
  require(lineNum % wayNum == 0)

  val setNum      = lineNum / wayNum
  val offsetWidth = log2Ceil(lineSize)
  val setWidth    = log2Ceil(setNum)
  val tagWidth    = awidth - setWidth - offsetWidth

  val io = IO(new ICacheIO(awidth, xlen) {
    val hit = if (sim) Some(DecoupledIO(Output(Bool()))) else None
  })
  val cacheLinesValid = RegInit(VecInit.fill(setNum, wayNum)(false.B))
  val cacheLinesTag   = Reg(Vec(setNum, Vec(wayNum, UInt(tagWidth.W))))
  val cacheLinesData  = Reg(Vec(setNum, Vec(wayNum, UInt((lineSize * 8).W))))

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
  val offset   = raddrReg(offsetWidth - 1, 0)
  val set      = raddrReg(setWidth + offsetWidth - 1, offsetWidth)
  val tag      = raddrReg(tagWidth + setWidth + offsetWidth - 1, setWidth + offsetWidth)

  val need       = WireDefault(needCache(raddrReg))
  val indexInSet = WireDefault(cacheLinesTag(set).onlyIndexWhere(_ === set))
  val cacheHit   = WireDefault(need && cacheLinesTag(set).contains(tag) && cacheLinesValid(set)(indexInSet))
  val cacheData  = WireDefault(cacheLinesData(set)(indexInSet))

  val availableIndex = cacheLinesValid(set).indexWhere(_ === false.B)
  cacheLinesValid(set)(availableIndex) := Mux(rfire && need, true.B, cacheLinesValid(set)(availableIndex))
  cacheLinesTag(set)(availableIndex)   := Mux(rfire && need, tag, cacheLinesTag(set)(availableIndex))
  cacheLinesData(set)(availableIndex)  := Mux(rfire && need, io.master.rdata, cacheLinesData(set)(availableIndex))
  val rdataReg = RegEnable(io.master.rdata, rfire)

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sCheck, sIdle),
      sCheck -> Mux(cacheHit, sSendBack, sSetAddr),
      sSetAddr -> Mux(arfire, sFetch, sSetAddr),
      sFetch -> Mux(rfire, sSendBack, sFetch),
      sSendBack -> Mux(io.out.fire, sIdle, sSendBack)
    )
  )

  assert(!rfire || io.master.rresp === "b00".U(2.W))
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
  io.master.araddr  := raddrReg
  io.master.arid    := 0.U
  io.master.arlen   := 0.U
  io.master.arsize  := "b010".U
  io.master.arburst := "b01".U

  io.master.rready := isFetch

  io.in.ready := isIdle

  io.out.valid      := isSendBack
  io.out.bits.rdata := Mux(need, cacheData(xlen - 1, 0), rdataReg)

  if (sim) {
    io.hit.get.valid := isCheck
    io.hit.get.bits  := cacheHit
  }
}
