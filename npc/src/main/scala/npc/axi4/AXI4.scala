package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._

object TransactionResponse extends ChiselEnum{
  val okey    = Value("b00".U)
  val exokay  = Value("b01".U)
  val slverr  = Value("b10".U)
  val decerr  = Value("b11".U)
}

class AXI4MasterIO extends AXI4LiteMasterIO {
  /* Write address channel */
  val awid     = Output(UInt(4.W))
  val awlen    = Output(UInt(8.W))
  val awsize   = Output(UInt(3.W))
  val awburst  = Output(UInt(2.W))

  /* Write data channel */
  val wlast    = Output(Bool())

  /* Write response channel */
  val bid      = Input(UInt(4.W))

  /* Read address channel */
  val arid     = Output(UInt(4.W))
  val arlen    = Output(UInt(8.W))
  val arsize   = Output(UInt(3.W))
  val arbrust  = Output(UInt(2.W))

  /* Read data channel */
  val rlast    = Input(Bool())
  val rid      = Input(UInt(4.W))
}

object AXI4Master {
  def nonTrans: AXI4MasterIO = {
    val nonTrans      = Wire(new AXI4MasterIO)
    /* Write address channel */
    nonTrans.awvalid  := false.B
    nonTrans.awaddr   := DontCare
    nonTrans.awid     := DontCare
    nonTrans.awlen    := DontCare
    nonTrans.awsize   := DontCare
    nonTrans.awburst  := DontCare
    /* Write data channel */
    nonTrans.wvalid   := false.B
    nonTrans.wdata    := DontCare
    nonTrans.wstarb   := DontCare
    nonTrans.wlast    := DontCare
    /* Write response channel */
    nonTrans.bready   := false.B
    /* Read address channel */
    nonTrans.arvalid  := false.B
    nonTrans.araddr   := DontCare
    nonTrans.arid     := DontCare
    nonTrans.arlen    := DontCare
    nonTrans.arsize   := DontCare
    nonTrans.arbrust  := DontCare
    /* Read data channel */
    nonTrans.rready   := false.B
    nonTrans
  }
}

class AXI4SlaveIO extends AXI4LiteSlaveIO {
  /* Write address channel */
  val awid     = Input(UInt(4.W))
  val awlen    = Input(UInt(8.W))
  val awsize   = Input(UInt(3.W))
  val awburst  = Input(UInt(2.W))

  /* Write data channel */
  val wlast    = Input(Bool())

  /* Write response channel */
  val bid      = Output(UInt(4.W))

  /* Read address channel */
  val arid     = Input(UInt(4.W))
  val arlen    = Input(UInt(8.W))
  val arsize   = Input(UInt(3.W))
  val arbrust  = Input(UInt(2.W))

  /* Read data channel */
  val rlast    = Output(Bool())
  val rid      = Output(UInt(4.W))
}

object AXI4Slave {
  def nonResp: AXI4SlaveIO = {
    val nonResp     = Wire(new AXI4SlaveIO)
    /* Write address channel */
    nonResp.awready := false.B
    /* Write data channel */
    nonResp.wready  := false.B
    /* Write response channel */
    nonResp.bvalid  := false.B
    nonResp.bresp   := DontCare
    nonResp.bid     := DontCare
    /* Read address channel */
    nonResp.arready := false.B
    /* Read data channel */
    nonResp.rvalid  := false.B
    nonResp.rresp   := DontCare
    nonResp.rdata   := DontCare
    nonResp.rlast   := DontCare
    nonResp.rid     := DontCare
    nonResp
  }
}

class AXI4Arbiter(masterNum: Int) extends Module {
  val io = IO(new Bundle {
    val masters  = Vec(masterNum, new AXI4SlaveIO)
    val slave    = new AXI4MasterIO
  })

  val writeRequests       = VecInit(io.masters.map(
    master => (master.awvalid || master.wvalid)
  )).asUInt
  val readRequests        = VecInit(io.masters.map(
    master => master.arvalid
  )).asUInt
  val requests            = writeRequests | readRequests
  val selected            = PriorityEncoder(requests)
  val isWriteTransaction  = writeRequests(selected)
  val isReadTransaction   = readRequests(selected)
  assert(!(isWriteTransaction && isReadTransaction))
  val selectedReg         = RegNext(selected, 0.U)
  val selectedMaster      = io.masters(selectedReg)
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

  val nonResp     = AXI4Slave.nonResp
  val nonTrans    = AXI4Master.nonTrans

  nonTrans <> io.slave
  for (i <- 0 until masterNum) {
    io.masters(i) <> nonResp
    when (i.U === selectedReg) {
      io.masters(i) <> io.slave
    }
  }
}

object AXI4Arbiter {
  def apply(masters: Seq[AXI4MasterIO]) = {
    val Arbiter = Module(new AXI4Arbiter(masters.length))
    for (i <- 0 until masters.length) {
      masters(i) <> Arbiter.io.masters(i)
    }
    Arbiter.io.slave
  }
}

class AXI4Xbar(slaves: Seq[Area]) extends Module {
  val length = slaves.length
  val io = IO(new Bundle {
    val master   = new AXI4SlaveIO
    val slaves   = Vec(length, new AXI4MasterIO)
  })

  val sIdle :: sWaitWaddr :: sWaitBresp :: sWaitRresp :: Nil = Enum(4)
  val state = RegInit(sIdle)

  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

  val isWaddrTransaction  = io.master.awvalid
  val isWdataTransaction  = io.master.wvalid
  val isReadTransaction   = io.master.arvalid
  assert(!((isWaddrTransaction || isWdataTransaction) && isReadTransaction))

  val awfire              = io.master.awvalid && io.master.awready
  val wfire               = io.master.wvalid && io.master.wready
  val bfire               = io.master.bvalid && io.master.bready
  val arfire              = io.master.arvalid && io.master.arready
  val rfire               = io.master.rvalid && io.master.rready

  state := MuxLookup(state, sIdle)(Seq(
    sIdle       -> MuxCase(sIdle, Seq(
      isWaddrTransaction  -> sWaitBresp,
      isWdataTransaction  -> sWaitWaddr,
      isReadTransaction   -> sWaitRresp,
    )),
    sWaitWaddr  -> Mux(isWaddrTransaction, sWaitBresp, sWaitWaddr),
    sWaitBresp  -> Mux(bfire, sIdle, sWaitBresp),
    sWaitRresp  -> Mux(rfire, sIdle, sWaitRresp),
  ))

  val transAddr         = RegInit(0.U(32.W))
  transAddr             := MuxCase(transAddr, Seq(
    (isIdle && isReadTransaction)       -> io.master.araddr,
    (isIdle && isWaddrTransaction)      -> io.master.awaddr,
    (isWaitWaddr && isWaddrTransaction) -> io.master.awaddr,
  ))
  val areas       = VecInit(slaves.map(area => VecInit(area.start.U, area.end.U)))
  val validSlaves = VecInit(areas.map(area => (transAddr >= area(0) && transAddr <= area(1)))).asUInt
  val haveSlave   = validSlaves.orR && (isWaitBresp || isWaitRresp)
  val slaveSelect = WireDefault(Mux(haveSlave, PriorityEncoder(validSlaves), 0.U))

  val nonTrans  = AXI4Master.nonTrans
  val nonResp   = AXI4Slave.nonResp

  io.master <> nonResp
  for (i <- 0 until slaves.length) {
    nonTrans <> io.slaves(i)
  }

  when (haveSlave) {
    io.master <> io.slaves(slaveSelect)
  }
}

object AXI4Xbar {
  def apply(
    master: AXI4MasterIO,
    slaves: Seq[(Area, AXI4SlaveIO)],
  ) = {
    val Xbar = Module(new AXI4Xbar(slaves.map(data => data._1)))
    master <> Xbar.io.master
    for (i <- 0 until slaves.length) {
      Xbar.io.slaves(i) <> slaves(i)._2
    }
  }
}

