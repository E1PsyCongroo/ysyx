package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

abstract class AXI4BundleBase(params: AXI4BundleParameters) extends Bundle

/**
  * Common signals of AW and AR channels of AXI4 protocol
  */
abstract class AXI4BundleA(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id     = UInt(params.idBits.W)
  val addr   = UInt(params.addrBits.W)
  val region = UInt(params.regionBits.W)
  val len    = UInt(params.lenBits.W) // number of beats - 1
  val size   = UInt(params.sizeBits.W) // bytes in beat = 2^size
  val burst  = UInt(params.burstBits.W)
  val lock   = UInt(params.lockBits.W)
  val cache  = UInt(params.cacheBits.W)
  val prot   = UInt(params.protBits.W)
  val qos    = UInt(params.qosBits.W) // 0=no QoS, bigger = higher priority

  // Number of bytes-1 in this operation
  def bytes1(x: Int = 0) = {
    val maxShift = 1 << params.sizeBits
    val tail     = ((BigInt(1) << maxShift) - 1).U
    (Cat(len, tail) << size) >> maxShift
  }
}

/**
  * AW channel of AXI4 protocol
  */
class AXI4BundleAW(params: AXI4BundleParameters) extends AXI4BundleA(params)

/**
  * AR channel of AXI4 protocol
  */
class AXI4BundleAR(params: AXI4BundleParameters) extends AXI4BundleA(params)

/**
  * W channel of AXI4 protocol
  */
class AXI4BundleW(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  // id ... removed in AXI4
  val data = UInt(params.dataBits.W)
  val strb = UInt((params.wstrBits).W)
  val last = UInt(params.wlastBits.W)
}

/**
  * R channel of AXI4 protocol
  */
class AXI4BundleR(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id   = UInt(params.idBits.W)
  val data = UInt(params.dataBits.W)
  val resp = UInt(params.rrespBits.W)
  val last = UInt(params.rlastBits.W)
}

/**
  * B channel of AXI4 protocol
  */
class AXI4BundleB(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val id   = UInt(params.idBits.W)
  val resp = UInt(params.brespBits.W)
}

/**
  * AXI4 protocol bundle
  */
class AXI4Bundle(params: AXI4BundleParameters) extends AXI4BundleBase(params) {
  val axi4params = params
  val aw         = Irrevocable(new AXI4BundleAW(params))
  val w          = Irrevocable(new AXI4BundleW(params))
  val b          = Flipped(Irrevocable(new AXI4BundleB(params)))
  val ar         = Irrevocable(new AXI4BundleAR(params))
  val r          = Flipped(Irrevocable(new AXI4BundleR(params)))
}

class StandardAXI4Bundle(params: AXI4BundleParameters) extends Bundle {
  val axi4params = params
  val awready    = Input(Bool())
  val awvalid    = Output(Bool())
  val awregion   = Output(UInt(params.regionBits.W))
  val awid       = Output(UInt(params.idBits.W))
  val awaddr     = Output(UInt(params.addrBits.W))
  val awlen      = Output(UInt(params.lenBits.W))
  val awsize     = Output(UInt(params.sizeBits.W))
  val awburst    = Output(UInt(params.burstBits.W))
  val awlock     = Output(UInt(params.lockBits.W))
  val awcache    = Output(UInt(params.cacheBits.W))
  val awprot     = Output(UInt(params.protBits.W))
  val awqos      = Output(UInt(params.qosBits.W))

  val wready = Input(Bool())
  val wvalid = Output(Bool())
  val wdata  = Output(UInt(params.dataBits.W))
  val wstrb  = Output(UInt(params.wstrBits.W))
  val wlast  = Output(UInt(params.wlastBits.W))

  val bready = Output(Bool())
  val bvalid = Input(Bool())
  val bid    = Input(UInt(params.idBits.W))
  val bresp  = Input(UInt(params.brespBits.W))

  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val arregion   = Output(UInt(params.regionBits.W))
  val arid    = Output(UInt(params.idBits.W))
  val araddr  = Output(UInt(params.addrBits.W))
  val arlen   = Output(UInt(params.lenBits.W))
  val arsize  = Output(UInt(params.sizeBits.W))
  val arburst = Output(UInt(params.burstBits.W))
  val arlock  = Output(UInt(params.lockBits.W))
  val arcache = Output(UInt(params.cacheBits.W))
  val arprot  = Output(UInt(params.protBits.W))
  val arqos   = Output(UInt(params.qosBits.W))

  val rready = Output(Bool())
  val rvalid = Input(Bool())
  val rid    = Input(UInt(params.idBits.W))
  val rdata  = Input(UInt(params.dataBits.W))
  val rresp  = Input(UInt(params.rrespBits.W))
  val rlast  = Input(UInt(params.rlastBits.W))
}

object AXI4Bundle {
  def apply(params: AXI4BundleParameters) = new AXI4Bundle(params)
  implicit val axiView: DataView[StandardAXI4Bundle, AXI4Bundle] = DataView(
    std => new AXI4Bundle(std.axi4params),
    // AW
    _.awready -> _.aw.ready,
    _.awvalid -> _.aw.valid,
    _.awregion -> _.aw.bits.region,
    _.awid -> _.aw.bits.id,
    _.awaddr -> _.aw.bits.addr,
    _.awlen -> _.aw.bits.len,
    _.awsize -> _.aw.bits.size,
    _.awburst -> _.aw.bits.burst,
    _.awlock -> _.aw.bits.lock,
    _.awcache -> _.aw.bits.cache,
    _.awprot -> _.aw.bits.prot,
    _.awqos -> _.aw.bits.qos,
    // W
    _.wready -> _.w.ready,
    _.wvalid -> _.w.valid,
    _.wdata -> _.w.bits.data,
    _.wstrb -> _.w.bits.strb,
    _.wlast -> _.w.bits.last,
    // B
    _.bready -> _.b.ready,
    _.bvalid -> _.b.valid,
    _.bid -> _.b.bits.id,
    _.bresp -> _.b.bits.resp,
    // AR
    _.arready -> _.ar.ready,
    _.arvalid -> _.ar.valid,
    _.arregion -> _.ar.bits.region,
    _.arid -> _.ar.bits.id,
    _.araddr -> _.ar.bits.addr,
    _.arlen -> _.ar.bits.len,
    _.arsize -> _.ar.bits.size,
    _.arburst -> _.ar.bits.burst,
    _.arlock -> _.ar.bits.lock,
    _.arcache -> _.ar.bits.cache,
    _.arprot -> _.ar.bits.prot,
    _.arqos -> _.ar.bits.qos,
    // R
    _.rready -> _.r.ready,
    _.rvalid -> _.r.valid,
    _.rid -> _.r.bits.id,
    _.rdata -> _.r.bits.data,
    _.rresp -> _.r.bits.resp,
    _.rlast -> _.r.bits.last
  )
}

object StandardAXI4Bundle {
  def apply(params: AXI4BundleParameters) = new StandardAXI4Bundle(params)
  implicit val axiView: DataView[AXI4Bundle, StandardAXI4Bundle] =
    AXI4Bundle.axiView.invert(axi => StandardAXI4Bundle(axi.axi4params))
}
