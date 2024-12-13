package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._

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
  val aw = Irrevocable(new AXI4BundleAW(params))
  val w  = Irrevocable(new AXI4BundleW(params))
  val b  = Flipped(Irrevocable(new AXI4BundleB(params)))
  val ar = Irrevocable(new AXI4BundleAR(params))
  val r  = Flipped(Irrevocable(new AXI4BundleR(params)))
}

object AXI4Bundle {
  def apply(params: AXI4BundleParameters) = new AXI4Bundle(params)
}
