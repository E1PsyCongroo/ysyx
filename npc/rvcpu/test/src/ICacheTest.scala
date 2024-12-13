import org.scalatest._

import rvcpu._
import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object Config {
  val axiParams = AXI4BundleParameters(
    addrBits          = 32,
    dataBits          = 32,
    idBits            = 4,
    regionPresent     = false,
    wlastPresent      = true,
    rlastPresent      = true,
    sizePresent       = true,
    lenPrenset        = true,
    burstPresent      = true,
    wstrbPresent      = true,
    brespPresent      = true,
    rrespPresent      = true,
    cachePrenset      = false,
    protPrensent      = false,
    qosPrensent       = false,
    exclusiveAccesses = false
  )
  val cpuConfig = CPUConfig(
    frequency  = BigInt("50000000"),
    xlen       = 32,
    extentionE = true,
    PCReset    = BigInt("80000000", 16),
    icacheConfig = ICacheConfig(
      totalWidth         = 6,
      blockWidth         = 5,
      associativityWidth = 0,
      icacheNeed         = AddressSet(BigInt("0", 16), BigInt("03ffffff", 16))
    ),
    sim = false
  )
}

class AXI4MemForICacheTest(params: AXI4BundleParameters) extends Module {
  require(params.dataBits == 32)
  require(params.lenPrenset)
  require(params.sizePresent)
  require(params.burstPresent)
  require(params.brespPresent)
  require(params.rlastPresent)
  require(params.rrespPresent)
  val size = params.dataBits / 8
  val io   = IO(Flipped(AXI4Bundle(params)))
  val mem  = Mem(BigInt("400000", 16), Vec(size, UInt(8.W)))

  val arfire = io.ar.fire
  val rfire  = io.r.fire

  val sReadIdle :: sSendRresp :: Nil  = Enum(2)

  val readState   = RegInit(sReadIdle)
  val isReadIdle  = readState === sReadIdle
  val isSendRresp = readState === sSendRresp

  readState := MuxLookup(readState, sReadIdle)(
    Seq(
      sReadIdle -> Mux(arfire, sSendRresp, sReadIdle),
      sSendRresp -> Mux(rfire && io.r.bits.last(0), sReadIdle, sSendRresp)
    )
  )

  /* Read address channel */
  val arready       = WireDefault(isReadIdle)
  val arid          = RegEnable(io.ar.bits.id, isReadIdle)
  val arlen         = RegEnable(io.ar.bits.len + 1.U, isReadIdle)
  val araddr        = RegEnable(io.ar.bits.addr, isReadIdle)
  val readCount     = RegInit(0.U(8.W))
  val nextReadCount = readCount + 1.U
  readCount := Mux(io.r.bits.last(0), 0.U, Mux(rfire, nextReadCount, readCount))
  val readData = Mux(isSendRresp, mem((araddr >> 2.U) + readCount).asUInt, 0.U)

  /* Read data channel */
  val delay      = 1.U(1.W)
  val delayCount = RegInit(0.U(delay.getWidth.W))
  val isFetch    = delayCount === delay - 1.U
  delayCount := MuxCase(
    delayCount,
    Seq(
      isReadIdle -> 0.U,
      (isSendRresp && !io.r.valid) -> (delayCount + 1.U)
    )
  )

  val rvalid = WireDefault(isFetch && isSendRresp)
  val rlast  = WireDefault(isSendRresp && isFetch && nextReadCount === arlen)
  val rdata  = WireDefault(readData)
  val rresp  = WireDefault(AXI4Parameters.RESP_OKAY)

  /* io.bind */
  io.aw.ready    := false.B
  io.aw.bits     := DontCare
  io.w.ready     := false.B
  io.w.bits      := DontCare
  io.b.valid     := false.B
  io.b.bits      := DontCare
  io.ar.ready    := arready
  io.r.valid     := rvalid
  io.r.bits      := DontCare
  io.r.bits.id   := arid
  io.r.bits.data := rdata
  io.r.bits.resp := rresp
  io.r.bits.last := isSendRresp && isFetch && nextReadCount === arlen
}


class ICacheTest extends Module {
  val io = IO(new Bundle {
    val req   = Flipped(DecoupledIO(new IFUOut(Config.cpuConfig)))
    val block = Input(Bool())
  })
  assume(io.req.bits.pc < 0x08000000.U)

  val mem = Module(new AXI4MemForICacheTest(Config.axiParams))
  val dut = Module(new ICache(Config.cpuConfig, Config.axiParams))

  val sIdle :: sSendRefReq :: sGetRefResp :: sGetDutResult :: sEnd :: Nil = Enum(5)

  val state          = RegInit(sIdle)
  val isIdle         = state === sIdle
  val isSendRefReq   = state === sSendRefReq
  val isGetRefResp   = state === sGetRefResp
  val isGetDutResult = state === sGetDutResult
  val isEnd          = state === sEnd

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.req.fire, sSendRefReq, sIdle),
      sSendRefReq -> Mux(mem.io.ar.fire, sGetRefResp, sSendRefReq),
      sGetRefResp -> Mux(mem.io.r.fire && mem.io.r.bits.last(0), sGetDutResult, sGetDutResult),
      sGetDutResult -> Mux(dut.io.out.fire, sEnd, sGetDutResult),
      sEnd -> sEnd
    )
  )
  io.req.ready := isIdle

  val in = RegEnable(io.req.bits, io.req.fire)
  mem.io                 := DontCare
  mem.io.aw.valid        := false.B
  mem.io.w.valid         := false.B
  mem.io.ar.valid        := false.B
  mem.io.b.ready         := false.B
  mem.io.r.ready         := false.B
  dut.io.master          := DontCare
  dut.io.master.aw.ready := false.B
  dut.io.master.w.ready  := false.B
  dut.io.master.b.valid  := false.B
  dut.io.master.ar.ready := false.B
  dut.io.master.r.valid  := false.B
  when(isSendRefReq || isGetRefResp) {
    mem.io.ar.valid      := isSendRefReq
    mem.io.ar.bits.addr  := in.pc
    mem.io.ar.bits.burst := AXI4Parameters.BURST_INCR
    mem.io.ar.bits.len   := 0.U
    mem.io.ar.bits.size  := "b10".U
    mem.io.ar.bits.id    := 1.U
    mem.io.r.ready       := isGetRefResp
    when(mem.io.r.fire) {
      assert(mem.io.r.bits.id === 1.U)
      assert(mem.io.r.bits.resp === AXI4Parameters.RESP_OKAY)
      assert(mem.io.r.bits.last === 1.U)
    }
  }.elsewhen(isGetDutResult) {
    mem.io <> dut.io.master
  }

  dut.io.clear     := false.B
  dut.io.in.valid  := sGetDutResult
  dut.io.in.bits   := in
  dut.io.out.ready := sGetDutResult
  val dutData = RegEnable(dut.io.out.bits.instruction, dut.io.out.fire)
  val refData = RegEnable(mem.io.r.bits.data, mem.io.r.fire && isGetRefResp)

  when(isEnd) {
    assert(dutData === refData)
  }
}

class ICacheFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("ICache")
  it should "pass" in {
    verify(new ICacheTest, Seq(BoundedCheck(20), BtormcEngineAnnotation))
  }
}
