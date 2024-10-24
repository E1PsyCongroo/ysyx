package rvcpu.dev.unuse

import rvcpu.dev._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class AXILiteSubordinateIO(awidth: Int = 32, dwidth: Int = 32) extends Bundle {
  /* Write address channel */
  val awvalid = Input(Bool())
  val awready = Output(Bool())
  val awid    = Input(UInt(0.W))
  val awaddr  = Input(UInt(awidth.W))
  val awport  = Input(UInt(3.W))

  /* Write data channel */
  val wvalid = Input(Bool())
  val wready = Output(Bool())
  val wdata  = Input(UInt(dwidth.W))
  val wstarb = Input(UInt((dwidth / 8).W))

  /* Write response channel */
  val bvalid = Output(Bool())
  val bready = Input(Bool())
  val bid    = Output(UInt(0.W))
  val bresp  = Output(UInt(2.W))

  /* Read address channel */
  val arvalid = Input(Bool())
  val arready = Output(Bool())
  val arid    = Input(UInt(0.W))
  val araddr  = Input(UInt(awidth.W))
  val arport  = Input(UInt(3.W))

  /* Read data channel */
  val rvalid = Output(Bool())
  val rready = Input(Bool())
  val rid    = Output(UInt(0.W))
  val rdata  = Output(UInt(dwidth.W))
  val rresp  = Output(UInt(2.W))
}

class AXILiteArbiter(awidth: Int = 32, dwidth: Int = 32, managerNum: Int) extends Module {
  val io = IO(new Bundle {
    val AXIManagers    = Vec(managerNum, new AXILiteSubordinateIO(awidth, dwidth))
    val AXISubordinate = Flipped(new AXILiteSubordinateIO(awidth, dwidth))
  })

  val writeRequests      = VecInit(io.AXIManagers.map(manager => (manager.awvalid || manager.wvalid))).asUInt
  val readRequests       = VecInit(io.AXIManagers.map(manager => manager.arvalid)).asUInt
  val requests           = writeRequests | readRequests
  val selected           = PriorityEncoder(requests)
  val isWriteTransaction = writeRequests(selected)
  val isReadTransaction  = readRequests(selected)
  assert(!(isWriteTransaction && isReadTransaction))
  val selectedReg     = RegEnable(selected, 0.U, isWriteTransaction || isReadTransaction)
  val selectedManager = io.AXIManagers(selectedReg)
  val bfire           = selectedManager.bvalid && selectedManager.bready
  val rfire           = selectedManager.rvalid && selectedManager.rready

  val sIdle :: sWaitWrite :: sWaitRead :: Nil = Enum(3)
  val state                                   = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          isWriteTransaction -> sWaitWrite,
          isReadTransaction -> sWaitRead
        )
      ),
      sWaitWrite -> Mux(bfire, sIdle, sWaitWrite),
      sWaitRead -> Mux(rfire, sIdle, sWaitRead)
    )
  )

  val isIdle      = state === sIdle
  val isWaitWrite = state === sWaitWrite
  val isWaitRead  = state === sWaitRead

  val nonResp = Wire(new AXILiteSubordinateIO(awidth, dwidth))
  /* Write address channel */
  nonResp.awready := false.B
  /* Write data channel */
  nonResp.wready := false.B
  /* Write response channel */
  nonResp.bvalid := false.B
  nonResp.bid    := DontCare
  nonResp.bresp  := DontCare
  /* Read address channel */
  nonResp.arready := false.B
  /* Read data channel */
  nonResp.rvalid := false.B
  nonResp.rid    := DontCare
  nonResp.rdata  := DontCare
  nonResp.rresp  := DontCare

  val nonTrans = Wire(Flipped(new AXILiteSubordinateIO(awidth, dwidth)))
  /* Write address channel */
  nonTrans.awvalid := false.B
  nonTrans.awid    := DontCare
  nonTrans.awaddr  := DontCare
  nonTrans.awport  := DontCare
  /* Write data channel */
  nonTrans.wvalid := false.B
  nonTrans.wdata  := DontCare
  nonTrans.wstarb := DontCare
  /* Write response channel */
  nonTrans.bready := false.B
  /* Read address channel */
  nonTrans.arvalid := false.B
  nonTrans.arid    := DontCare
  nonTrans.araddr  := DontCare
  nonTrans.arport  := DontCare
  /* Read data channel */
  nonTrans.rready := false.B

  nonTrans <> io.AXISubordinate
  for (i <- 0 until managerNum) {
    io.AXIManagers(i) <> nonResp
    when(i.U === selectedReg) {
      io.AXIManagers(i) <> io.AXISubordinate
    }
  }
}

object AXILiteArbiter {
  def apply(awidth: Int, dwidth: Int, managers: Seq[AXILiteSubordinateIO]) = {
    val Arbiter = Module(new AXILiteArbiter(awidth, dwidth, managers.length))
    for (i <- 0 until managers.length) {
      managers(i) <> Arbiter.io.AXIManagers(i)
    }
    Arbiter.io.AXISubordinate
  }
}

class AXILiteXbar(
  awidth:       Int = 32,
  dwidth:       Int = 32,
  managerNum:   Int,
  subordinates: Seq[Area])
    extends Module {
  val io = IO(new Bundle {
    val AXIManagers     = Vec(managerNum, new AXILiteSubordinateIO(awidth, dwidth))
    val AXISubordinates = Vec(subordinates.length, Flipped(new AXILiteSubordinateIO(awidth, dwidth)))
  })

  val sIdle :: sWaitWaddr :: sWaitWdata :: sWaitBresp :: sWaitRresp :: Nil = Enum(5)
  val state                                                                = RegInit(sIdle)

  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitWdata = state === sWaitWdata
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

  val waddrRequests      = VecInit(io.AXIManagers.map(manager => manager.awvalid)).asUInt
  val wdataRequests      = VecInit(io.AXIManagers.map(manager => manager.wvalid)).asUInt
  val readRequests       = VecInit(io.AXIManagers.map(manager => manager.arvalid)).asUInt
  val requests           = waddrRequests | wdataRequests | readRequests
  val managerSelect      = PriorityEncoder(requests)
  val isWaddrTransaction = waddrRequests(managerSelect)
  val isWdataTransaction = wdataRequests(managerSelect)
  val isReadTransaction  = readRequests(managerSelect)
  assert(!((isWaddrTransaction || isWdataTransaction) && isReadTransaction))
  val managerSelectReg = RegInit(0.U)
  val selectedManager  = io.AXIManagers(managerSelectReg)
  val awfire           = selectedManager.awvalid && selectedManager.awready
  val wfire            = selectedManager.wvalid && selectedManager.wready
  val bfire            = selectedManager.bvalid && selectedManager.bready
  val rfire            = selectedManager.rvalid && selectedManager.rready

  managerSelectReg := MuxCase(
    managerSelectReg,
    Seq(
      isIdle -> managerSelect,
      (isWaitBresp && bfire) -> 0.U,
      (isWaitRresp && rfire) -> 0.U
    )
  )

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          (isWaddrTransaction && isWdataTransaction) -> sWaitBresp,
          isWaddrTransaction -> sWaitWdata,
          isWdataTransaction -> sWaitWaddr,
          isReadTransaction -> sWaitRresp
        )
      ),
      sWaitWaddr -> Mux(awfire, sWaitBresp, sWaitWaddr),
      sWaitWdata -> Mux(wfire, sWaitBresp, sWaitWdata),
      sWaitBresp -> Mux(bfire, sIdle, sWaitBresp),
      sWaitRresp -> Mux(rfire, sIdle, sWaitRresp)
    )
  )

  val transAddr = RegInit(0.U(dwidth.W))
  transAddr := MuxCase(
    transAddr,
    Seq(
      (isIdle && isReadTransaction) -> io.AXIManagers(managerSelect).araddr,
      (isIdle && isWaddrTransaction) -> io.AXIManagers(managerSelect).awaddr,
      (isWaitWaddr && isWaddrTransaction) -> selectedManager.awaddr
    )
  )
  val validSubordinates = VecInit(subordinates.map(area => area.in(transAddr))).asUInt
  val subordinateSelect = WireDefault(
    Mux(
      isWaitWdata || isWaitBresp || isWaitRresp,
      Mux(validSubordinates =/= 0.U, PriorityEncoder(validSubordinates), 0.U),
      0.U(log2Ceil(subordinates.length).W)
    )
  )

  val nonResp = Wire(new AXILiteSubordinateIO(awidth, dwidth))
  /* Write address channel */
  nonResp.awready := false.B
  /* Write data channel */
  nonResp.wready := false.B
  /* Write response channel */
  nonResp.bvalid := false.B
  nonResp.bid    := DontCare
  nonResp.bresp  := DontCare
  /* Read address channel */
  nonResp.arready := false.B
  /* Read data channel */
  nonResp.rvalid := false.B
  nonResp.rid    := DontCare
  nonResp.rdata  := DontCare
  nonResp.rresp  := DontCare

  val nonTrans = Wire(Flipped(new AXILiteSubordinateIO(awidth, dwidth)))
  /* Write address channel */
  nonTrans.awvalid := false.B
  nonTrans.awid    := DontCare
  nonTrans.awaddr  := DontCare
  nonTrans.awport  := DontCare
  /* Write data channel */
  nonTrans.wvalid := false.B
  nonTrans.wdata  := DontCare
  nonTrans.wstarb := DontCare
  /* Write response channel */
  nonTrans.bready := false.B
  /* Read address channel */
  nonTrans.arvalid := false.B
  nonTrans.arid    := DontCare
  nonTrans.araddr  := DontCare
  nonTrans.arport  := DontCare
  /* Read data channel */
  nonTrans.rready := false.B

  for (i <- 0 until managerNum) {
    io.AXIManagers(i) <> nonResp
  }
  for (i <- 0 until subordinates.length) {
    nonTrans <> io.AXISubordinates(i)
  }

  when(isWaitWdata || isWaitBresp || isWaitRresp) {
    io.AXIManagers(managerSelectReg) <> io.AXISubordinates(subordinateSelect)
  }
}

object AXILiteXbar {
  def apply(
    awidth:       Int,
    dwidth:       Int,
    managers:     Seq[AXILiteSubordinateIO],
    subordinates: Seq[(Area, AXILiteSubordinateIO)]
  ) = {
    val Xbar = Module(new AXILiteXbar(awidth, dwidth, managers.length, subordinates.map(data => data._1)))
    for (i <- 0 until managers.length) {
      managers(i) <> Xbar.io.AXIManagers(i)
    }
    for (i <- 0 until subordinates.length) {
      Xbar.io.AXISubordinates(i) <> subordinates(i)._2
    }
  }
}
