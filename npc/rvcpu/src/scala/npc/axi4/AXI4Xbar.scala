package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._

object AXI4Arbiter {
  type Policy = (Int, UInt, Bool) => UInt

  val lowestIndexFirst:  Policy = (width, valids, select) => ~(scanLeftOr(valids) << 1)(width - 1, 0)
  val highestIndexFirst: Policy = (width, valids, select) => ~((scanRightOr(valids) >> 1).pad(width))

  def apply[T <: Data](policy: Policy)(sink: IrrevocableIO[T], sources: IrrevocableIO[T]*): Unit = {
    if (sources.isEmpty) {
      sink.valid := false.B
    } else {
      returnWinner(policy)(sink, sources: _*)
    }
  }

  def returnWinner[T <: Data](policy: Policy)(sink: IrrevocableIO[T], sources: IrrevocableIO[T]*) = {
    require(!sources.isEmpty)

    // The arbiter is irrevocable; when !idle, repeat last request
    val idle = RegInit(true.B)

    // Who wants access to the sink?
    val valids   = sources.map(_.valid)
    val anyValid = valids.reduce(_ || _)
    // Arbitrate amongst the requests
    val readys = VecInit(policy(valids.length, Cat(valids.reverse), idle).asBools)
    // Which request wins arbitration?
    val winner = VecInit((readys.zip(valids)).map { case (r, v) => r && v })

    // Confirm the policy works properly
    require(readys.size == valids.size)
    // Never two winners
    val prefixOR = winner.scanLeft(false.B)(_ || _).init
    assert((prefixOR.zip(winner)).map { case (p, w) => !p || !w }.reduce { _ && _ })
    // If there was any request, there is a winner
    assert(!anyValid || winner.reduce(_ || _))

    // The one-hot source granted access in the previous cycle
    val state    = RegInit(VecInit.fill(sources.size)(false.B))
    val muxState = Mux(idle, winner, state)
    state := muxState

    // Determine when we go idle
    idle := Mux(sink.fire, true.B, Mux(anyValid, false.B, idle))

    if (sources.size > 1) {
      // 没有发出请求的 source 可以置位 ready，使用 readys 可以缩短路径，降低延迟
      val allowed = Mux(idle, readys, state)
      (sources.zip(allowed)).foreach {
        case (s, r) =>
          s.ready := sink.ready && r
      }
    } else {
      sources(0).ready := sink.ready
    }

    sink.valid := Mux(idle, anyValid, Mux1H(state, valids))
    sink.bits :<= Mux1H(muxState, sources.map(_.bits))
    muxState
  }
}

class AXI4Interconnect(
  params:      AXI4BundleParameters,
  fanInNum:    Int,
  fanOutArea:  Seq[Seq[AddressSet]],
  fanOutTrans: Seq[Boolean])
    extends Module {
  require(fanOutArea.length == fanOutTrans.length)
  val fanOutNum = fanOutArea.length

  val io = IO(new Bundle {
    val fanIn  = Vec(fanInNum, Flipped(AXI4Bundle(params)))
    val fanOut = Vec(fanOutNum, AXI4Bundle(params))
  })

  val sWriteIdle :: sWaitBresp :: Nil = Enum(2)
  val sReadIdle :: sWaitRresp :: Nil  = Enum(2)

  val writeState  = RegInit(sWriteIdle)
  val isWriteIdle = writeState === sWriteIdle
  val isWaitBresp = writeState === sWaitBresp

  val readState   = RegInit(sReadIdle)
  val isReadIdle  = readState === sReadIdle
  val isWaitRresp = readState === sWaitRresp

  val writeRequests = VecInit(io.fanIn.map(in => in.aw.valid)).asUInt
  val readRequests  = VecInit(io.fanIn.map(in => in.ar.valid)).asUInt

  val writeSelected    = PriorityEncoder(writeRequests)
  val writeSelectedReg = RegEnable(writeSelected, isWriteIdle)
  val write            = writeRequests =/= 0.U

  val readSelected    = PriorityEncoder(readRequests)
  val readSelectedReg = RegEnable(readSelected, isReadIdle)
  val read            = readRequests =/= 0.U

  val awfire = io.fanIn(writeSelectedReg).aw.fire
  val wfire  = io.fanIn(writeSelectedReg).w.fire
  val bfire  = io.fanIn(writeSelectedReg).b.fire
  val arfire = io.fanIn(readSelectedReg).ar.fire
  val rfire  = io.fanIn(readSelectedReg).r.fire
  val rlast  = io.fanIn(readSelectedReg).r.bits.last(0)

  writeState := MuxLookup(writeState, sWriteIdle)(
    Seq(
      sWriteIdle -> Mux(write, sWaitBresp, sWriteIdle),
      sWaitBresp -> Mux(bfire, sWriteIdle, sWaitBresp)
    )
  )

  readState := MuxLookup(readState, sReadIdle)(
    Seq(
      sReadIdle -> Mux(read, sWaitRresp, sReadIdle),
      sWaitRresp -> Mux(rfire && rlast, sReadIdle, sWaitRresp)
    )
  )

  val awaddr         = io.fanIn(writeSelected).aw.bits.addr
  val wmatches       = VecInit(fanOutArea.map(_.foldLeft(false.B)(_ || _.contains(awaddr)))).asUInt
  val writeOutSelect = RegEnable(PriorityEncoder(wmatches), isWriteIdle)

  val araddr        = io.fanIn(readSelected).ar.bits.addr
  val rmatches       = VecInit(fanOutArea.map(_.foldLeft(false.B)(_ || _.contains(araddr)))).asUInt
  val readOutSelect = RegEnable(PriorityEncoder(rmatches), isReadIdle)

  for (i <- 0 until fanInNum) {
    io.fanIn(i).aw.ready := (i.U === writeSelectedReg) && isWaitBresp && io.fanOut(writeOutSelect).aw.ready
    io.fanIn(i).w.ready  := (i.U === writeSelectedReg) && isWaitBresp && io.fanOut(writeOutSelect).w.ready
    io.fanIn(i).b.valid  := (i.U === writeSelectedReg) && isWaitBresp && io.fanOut(writeOutSelect).b.valid
    io.fanIn(i).b.bits   := io.fanOut(writeOutSelect).b.bits
    io.fanIn(i).ar.ready := (i.U === readSelectedReg) && isWaitRresp && io.fanOut(readOutSelect).ar.ready
    io.fanIn(i).r.valid  := (i.U === readSelectedReg) && isWaitRresp && io.fanOut(readOutSelect).r.valid
    io.fanIn(i).r.bits   := io.fanOut(readOutSelect).r.bits
  }
  for (i <- 0 until fanOutNum) {
    io.fanOut(i).aw.valid := (i.U === writeOutSelect) && isWaitBresp && io.fanIn(writeSelectedReg).aw.valid
    io.fanOut(i).aw.bits  := io.fanIn(writeSelectedReg).aw.bits
    if (fanOutTrans(i)) {
      io.fanOut(i).aw.bits.addr := io.fanIn(writeSelectedReg).aw.bits.addr ^ fanOutArea(i).minBy(_.base).base.U
    }
    io.fanOut(i).w.valid  := (i.U === writeOutSelect) && isWaitBresp && io.fanIn(writeSelectedReg).w.valid
    io.fanOut(i).w.bits   := io.fanIn(writeSelectedReg).w.bits
    io.fanOut(i).b.ready  := (i.U === writeOutSelect) && isWaitBresp && io.fanIn(writeSelectedReg).b.ready
    io.fanOut(i).ar.valid := (i.U === readOutSelect) && isWaitRresp && io.fanIn(readSelectedReg).ar.valid
    io.fanOut(i).ar.bits  := io.fanIn(readSelectedReg).ar.bits
    if (fanOutTrans(i)) {
      io.fanOut(i).ar.bits.addr := io.fanIn(readSelectedReg).ar.bits.addr ^ fanOutArea(i).minBy(_.base).base.U
    }
    io.fanOut(i).r.ready := (i.U === readOutSelect) && isWaitRresp && io.fanIn(readSelectedReg).r.ready
  }
}

object AXI4Interconnect {
  def apply(
    params:      AXI4BundleParameters,
    fanIn:       Seq[AXI4Bundle],
    fanOut:      Seq[AXI4Bundle],
    fanOutArea:  Seq[Seq[AddressSet]],
    fanOutTrans: Seq[Boolean]
  ) = {
    val AXI4Interconnect = Module(new AXI4Interconnect(params, fanIn.length, fanOutArea, fanOutTrans))
    for (i <- 0 until fanIn.length) {
      AXI4Interconnect.io.fanIn(i) <> fanIn(i)
    }
    for (i <- 0 until fanOut.length) {
      AXI4Interconnect.io.fanOut(i) <> fanOut(i)
    }
  }
}

// class AXI4Xbar(
//   fanInParams:  Seq[AXI4BundleParameters],
//   fanOutParams: Seq[AXI4BundleParameters],
//   fanOutArea:   Seq[Area],
//   fanOutTrans:  Seq[Boolean])
//     extends Module {
//   require(fanOutArea.length == fanOutTrans.length)
//   val fanInNum  = fanInParams.length
//   val fanOutNum = fanOutArea.length

//   val io = IO(new Bundle {
//     val fanIn  = (0 until finInNum).map(i => Flipped(AXI4Bundle(fanInParams(i)))).toSeq
//     val fanOut = (0 until finOutNum).map(i => AXI4Bundle(fanOutParams(i))).toSeq
//   })

//   val requestARIO = io.fanIn.map { i => VecInit(fanOutArea.map { area => area.in(i.ar.bits.addr) }) }
//   val requestAWIO = io.fanIn.map { i => VecInit(fanOutArea.map { area => area.in(i.aw.bits.addr) }) }

//   // We need an intermediate size of bundle with the widest possible identifiers
//   val wide_bundle = AXI4BundleParameters.union(fanInParams.map(_.params) ++ fanOutParams.map(_.params))

//   // Transform input bundles
//   val in = Wire(Vec(fanInNum, new AXI4Bundle(wide_bundle)))
//   for (i <- 0 until fanInNum) {
//     in(i).squeezeAll.waiveAll :<>= fanIn(i).squeezeAll.waiveAll

//     in(i).aw.bits.id      := io.fanIn(i).aw.bits.id
//     in(i).ar.bits.id      := io.fanIn(i).ar.bits.id
//     io.fanIn(i).r.bits.id := in(i).r.bits.id
//     io.fanIn(i).b.bits.id := in(i).b.bits.id
//   }

//   // Transform output bundles
//   val out = Wire(Vec(fanOutNum, new AXI4Bundle(wide_bundle)))
//   for (i <- 0 until fanOutNum) {
//     io.fanOut(i).squeezeAll.waiveAll :<>= out(i).squeezeAll.waiveAll

//     if (io_in.size > 1) {
//       // Block AW if we cannot record the W source
//       val latched = RegInit(false.B) // cut awOut(i).enq.valid from awready
//       io_out(i).aw.valid    := out(i).aw.valid && (latched || awOut(i).io.enq.ready)
//       out(i).aw.ready       := io_out(i).aw.ready && (latched || awOut(i).io.enq.ready)
//       awOut(i).io.enq.valid := out(i).aw.valid && !latched
//       when(awOut(i).io.enq.fire) { latched := true.B }
//       when(out(i).aw.fire) { latched := false.B }

//       // Block W if we do not have an AW source
//       io_out(i).w.valid     := out(i).w.valid && awOut(i).io.deq.valid // depends on awvalid (but not awready)
//       out(i).w.ready        := io_out(i).w.ready && awOut(i).io.deq.valid
//       awOut(i).io.deq.ready := out(i).w.valid && out(i).w.bits.last && io_out(i).w.ready
//     } else {
//       awOut(i).io := DontCare // aw out queue is not used when io_in.size == 1
//     }
//   }

//   // Transform output bundles
//   val out = Wire(Vec(io_out.size, new AXI4Bundle(wide_bundle)))
//   for (i <- 0 until out.size) {
//     out(i).b.bits.user := DontCare
//     out(i).r.bits.user := DontCare
//     io_out(i).squeezeAll.waiveAll :<>= out(i).squeezeAll.waiveAll

//     if (io_in.size > 1) {
//       // Block AW if we cannot record the W source
//       val latched = RegInit(false.B) // cut awOut(i).enq.valid from awready
//       io_out(i).aw.valid    := out(i).aw.valid && (latched || awOut(i).io.enq.ready)
//       out(i).aw.ready       := io_out(i).aw.ready && (latched || awOut(i).io.enq.ready)
//       awOut(i).io.enq.valid := out(i).aw.valid && !latched
//       when(awOut(i).io.enq.fire) { latched := true.B }
//       when(out(i).aw.fire) { latched := false.B }

//       // Block W if we do not have an AW source
//       io_out(i).w.valid     := out(i).w.valid && awOut(i).io.deq.valid // depends on awvalid (but not awready)
//       out(i).w.ready        := io_out(i).w.ready && awOut(i).io.deq.valid
//       awOut(i).io.deq.ready := out(i).w.valid && out(i).w.bits.last && io_out(i).w.ready
//     } else {
//       awOut(i).io := DontCare // aw out queue is not used when io_in.size == 1
//     }
//   }

// }

// object AXI4Xbar {
//   def apply(
//     fanIn:       Seq[AXI4Bundle],
//     fanOut:      Seq[AXI4Bundle],
//     fanOutArea:  Seq[Area],
//     fanOutTrans: Seq[Boolean]
//   ) = {
//     val xbar = Module(new AXI4Xbar(fanIn.length, fanOutArea, fanOutTrans))
//     for (i <- 0 until fanIn.length) {
//       xbar.io.fanIn(i) <> fanIn(i)
//     }
//     for (i <- 0 until fanOut.length) {
//       xbar.io.fanOut(i) <> fanOut(i)
//     }
//     xbar
//   }
// }
