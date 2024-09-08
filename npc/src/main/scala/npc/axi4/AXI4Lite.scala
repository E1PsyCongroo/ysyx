package rvcpu.axi4

import chisel3._
import chisel3.util._
import rvcpu.dev._

class AXI4LiteMasterIO extends Bundle {
  /* Write address channel */
  val awready  = Input(Bool())
  val awvalid  = Output(Bool())
  val awaddr   = Output(UInt(32.W))

  /* Write data channel */
  val wready   = Input(Bool())
  val wvalid   = Output(Bool())
  val wdata    = Output(UInt(32.W))
  val wstarb   = Output(UInt(4.W))

  /* Write response channel */
  val bready   = Output(Bool())
  val bvalid   = Input(Bool())
  val bresp    = Input(UInt(2.W))

  /* Read address channel */
  val arready  = Input(Bool())
  val arvalid  = Output(Bool())
  val araddr   = Output(UInt(32.W))

  /* Read data channel */
  val rready   = Output(Bool())
  val rvalid   = Input(Bool())
  val rresp    = Input(UInt(2.W))
  val rdata    = Input(UInt(32.W))
}

class AXI4LiteSlaveIO extends Bundle {
  /* Write address channel */
  val awready  = Output(Bool())
  val awvalid  = Input(Bool())
  val awaddr   = Input(UInt(32.W))

  /* Write data channel */
  val wready   = Output(Bool())
  val wvalid   = Input(Bool())
  val wdata    = Input(UInt(32.W))
  val wstarb   = Input(UInt(4.W))

  /* Write response channel */
  val bready   = Input(Bool())
  val bvalid   = Output(Bool())
  val bresp    = Output(UInt(2.W))

  /* Read address channel */
  val arready  = Output(Bool())
  val arvalid  = Input(Bool())
  val araddr   = Input(UInt(32.W))

  /* Read data channel */
  val rready   = Input(Bool())
  val rvalid   = Output(Bool())
  val rresp    = Output(UInt(2.W))
  val rdata    = Output(UInt(32.W))
}

class AXI4LiteArbiter(masterNum: Int) extends Module {
  val io = IO(new Bundle {
    val AXIMasters  = Vec(masterNum, new AXI4LiteSlaveIO)
    val AXISlave    = new AXI4LiteMasterIO
  })

  val writeRequests       = VecInit(io.AXIMasters.map(
    master => (master.awvalid || master.wvalid)
  )).asUInt
  val readRequests        = VecInit(io.AXIMasters.map(
    master => master.arvalid
  )).asUInt
  val requests            = writeRequests | readRequests
  val selected            = PriorityEncoder(requests)
  val isWriteTransaction  = writeRequests(selected)
  val isReadTransaction   = readRequests(selected)
  assert(!(isWriteTransaction && isReadTransaction))
  val selectedReg         = RegEnable(selected, 0.U, isWriteTransaction || isReadTransaction)
  val selectedMaster     = io.AXIMasters(selectedReg)
  val bfire               = selectedMaster.bvalid && selectedMaster.bready
  val rfire               = selectedMaster.rvalid && selectedMaster.rready

  val sIdle :: sWaitWrite :: sWaitRead :: Nil = Enum(3)
  val state = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(Seq(
    sIdle       -> MuxCase(sIdle, Seq(
      isWriteTransaction  -> sWaitWrite,
      isReadTransaction   -> sWaitRead,
    )),
    sWaitWrite  -> Mux(bfire, sIdle, sWaitWrite),
    sWaitRead   -> Mux(rfire, sIdle, sWaitRead),
  ))

  val isIdle      = state === sIdle
  val isWaitWrite = state === sWaitWrite
  val isWaitRead  = state === sWaitRead


  val nonResp     = Wire(new AXI4LiteSlaveIO)
  /* Write address channel */
  nonResp.awready := false.B
  /* Write data channel */
  nonResp.wready  := false.B
  /* Write response channel */
  nonResp.bvalid  := false.B
  nonResp.bready  := false.B
  /* Read address channel */
  nonResp.arready := false.B
  /* Read data channel */
  nonResp.rvalid  := false.B
  nonResp.rdata   := DontCare
  nonResp.rresp   := DontCare

  val nonTrans      = Wire((new AXI4LiteMasterIO))
  /* Write address channel */
  nonTrans.awvalid  := false.B
  nonTrans.awaddr   := DontCare
  /* Write data channel */
  nonTrans.wvalid   := false.B
  nonTrans.wdata    := DontCare
  nonTrans.wstarb   := DontCare
  /* Write response channel */
  nonTrans.bready   := false.B
  /* Read address channel */
  nonTrans.arvalid  := false.B
  nonTrans.araddr   := DontCare
  /* Read data channel */
  nonTrans.rready   := false.B

  nonTrans <> io.AXISlave
  for (i <- 0 until masterNum) {
    io.AXIMasters(i) <> nonResp
    when (i.U === selectedReg) {
      io.AXIMasters(i) <> io.AXISlave
    }
  }
}

object AXI4LiteArbiter {
  def apply(masters: Seq[AXI4LiteMasterIO]) = {
    val Arbiter = Module(new AXI4LiteArbiter(masters.length))
    for (i <- 0 until masters.length) {
      masters(i) <> Arbiter.io.AXIMasters(i)
    }
    Arbiter.io.AXISlave
  }
}

class AXI4LiteInterconnect(
  masterNum:Int,
  slaves: Seq[Area]
) extends Module {
  val io = IO(new Bundle {
    val AXIMasters  = Vec(masterNum, new AXI4LiteSlaveIO)
    val AXISlaves   = Vec(slaves.length, new AXI4LiteMasterIO)
  })

  val sIdle :: sWaitWaddr :: sWaitWdata :: sWaitBresp :: sWaitRresp :: Nil = Enum(5)
  val state = RegInit(sIdle)

  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitWdata = state === sWaitWdata
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

  val waddrRequests       = VecInit(io.AXIMasters.map(
    master => master.awvalid
  )).asUInt
  val wdataRequests       = VecInit(io.AXIMasters.map(
    master => master.wvalid
  )).asUInt
  val readRequests        = VecInit(io.AXIMasters.map(
    master => master.arvalid
  )).asUInt
  val requests            = waddrRequests | wdataRequests | readRequests
  val masterSelect        = PriorityEncoder(requests)
  val isWaddrTransaction  = waddrRequests(masterSelect)
  val isWdataTransaction  = wdataRequests(masterSelect)
  val isReadTransaction   = readRequests(masterSelect)
  assert(!((isWaddrTransaction || isWdataTransaction) && isReadTransaction))
  val masterSelectReg    = RegInit(0.U)
  val selectedMaster     = io.AXIMasters(masterSelectReg)
  val awfire              = selectedMaster.awvalid && selectedMaster.awready
  val wfire               = selectedMaster.wvalid && selectedMaster.wready
  val bfire               = selectedMaster.bvalid && selectedMaster.bready
  val rfire               = selectedMaster.rvalid && selectedMaster.rready

  masterSelectReg        := MuxCase(masterSelectReg, Seq(
    isIdle                  -> masterSelect,
    (isWaitBresp && bfire)  -> 0.U,
    (isWaitRresp && rfire)  -> 0.U,
  ))

  state := MuxLookup(state, sIdle)(Seq(
    sIdle       -> MuxCase(sIdle, Seq(
      (isWaddrTransaction && isWdataTransaction)  -> sWaitBresp,
      isWaddrTransaction                          -> sWaitWdata,
      isWdataTransaction                          -> sWaitWaddr,
      isReadTransaction                           -> sWaitRresp,
    )),
    sWaitWaddr  -> Mux(awfire, sWaitBresp, sWaitWaddr),
    sWaitWdata  -> Mux(wfire, sWaitBresp, sWaitWdata),
    sWaitBresp  -> Mux(bfire, sIdle, sWaitBresp),
    sWaitRresp  -> Mux(rfire, sIdle, sWaitRresp),
  ))

  val transAddr         = RegInit(0.U(32.W))
  transAddr             := MuxCase(transAddr, Seq(
    (isIdle && isReadTransaction)       -> io.AXIMasters(masterSelect).araddr,
    (isIdle && isWaddrTransaction)      -> io.AXIMasters(masterSelect).awaddr,
    (isWaitWaddr && isWaddrTransaction) -> selectedMaster.awaddr,
  ))
  val areas       = VecInit(slaves.map(area => VecInit(area.start.U, area.end.U)))
  val validSlaves = VecInit(areas.map(area => (transAddr >= area(0) && transAddr <= area(1)))).asUInt
  val slaveselect = WireDefault(Mux(
    isWaitWdata || isWaitBresp || isWaitRresp,
    Mux(validSlaves =/= 0.U, PriorityEncoder(validSlaves), 0.U),
    0.U(log2Ceil(slaves.length).W),
  ))

  val nonResp     = Wire(new AXI4LiteSlaveIO)
  /* Write address channel */
  nonResp.awready := false.B
  /* Write data channel */
  nonResp.wready  := false.B
  /* Write response channel */
  nonResp.bvalid  := false.B
  nonResp.bresp   := DontCare
  /* Read address channel */
  nonResp.arready := false.B
  /* Read data channel */
  nonResp.rvalid  := false.B
  nonResp.rdata   := DontCare
  nonResp.rresp   := DontCare

  val nonTrans      = Wire(new AXI4LiteMasterIO)
  /* Write address channel */
  nonTrans.awvalid  := false.B
  nonTrans.awaddr   := DontCare
  /* Write data channel */
  nonTrans.wvalid   := false.B
  nonTrans.wdata    := DontCare
  nonTrans.wstarb   := DontCare
  /* Write response channel */
  nonTrans.bready   := false.B
  /* Read address channel */
  nonTrans.arvalid  := false.B
  nonTrans.araddr   := DontCare
  /* Read data channel */
  nonTrans.rready   := false.B

  for (i <- 0 until masterNum) {
    io.AXIMasters(i) <> nonResp
  }
  for (i <- 0 until slaves.length) {
    nonTrans <> io.AXISlaves(i)
  }

  when (isWaitWdata || isWaitBresp || isWaitRresp) {
    io.AXIMasters(masterSelectReg) <> io.AXISlaves(slaveselect)
  }
}

object AXI4LiteInterconnect {
  def apply(
    masters: Seq[AXI4LiteMasterIO],
    slaves: Seq[(Area, AXI4LiteSlaveIO)],
  ) = {
    val Interconnect = Module(new AXI4LiteInterconnect(masters.length, slaves.map(data => data._1)))
    for (i <- 0 until masters.length) {
      masters(i) <> Interconnect.io.AXIMasters(i)
    }
    for (i <- 0 until slaves.length) {
      Interconnect.io.AXISlaves(i) <> slaves(i)._2
    }
  }
}
