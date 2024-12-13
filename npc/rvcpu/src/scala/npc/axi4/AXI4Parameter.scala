package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._
import scala.math._

object AXI4Parameters {
  // These are all fixed by the AXI4 standard:
  val wlastBits  = 1
  val rlastBIts  = 1
  val regionBits = 4
  val lenBits    = 8
  val sizeBits   = 3
  val burstBits  = 2
  val respBits   = 2
  val lockBits   = 1
  val cacheBits  = 4
  val protBits   = 3
  val qosBits    = 4

  def CACHE_RALLOCATE  = 8.U(cacheBits.W)
  def CACHE_WALLOCATE  = 4.U(cacheBits.W)
  def CACHE_MODIFIABLE = 2.U(cacheBits.W)
  def CACHE_BUFFERABLE = 1.U(cacheBits.W)

  def PROT_PRIVILEGED  = 1.U(protBits.W)
  def PROT_INSECURE    = 2.U(protBits.W)
  def PROT_INSTRUCTION = 4.U(protBits.W)

  def BURST_FIXED = 0.U(burstBits.W)
  def BURST_INCR  = 1.U(burstBits.W)
  def BURST_WRAP  = 2.U(burstBits.W)

  def RESP_OKAY   = 0.U(respBits.W)
  def RESP_EXOKAY = 1.U(respBits.W)
  def RESP_SLVERR = 2.U(respBits.W)
  def RESP_DECERR = 3.U(respBits.W)
}

// /**
//   * Parameters for AXI4 slave
//   *
//   * @param address base address
//   * @param resources device tree resource
//   * @param regionType memory region type
//   * @param executable whether processor can execute from this memory
//   */
// case class AXI4SlaveParameters(
//   address:       Seq[AddressSet],
//   resources:     Seq[Resource] = Nil,
//   regionType:    RegionType.T  = RegionType.GET_EFFECTS,
//   executable:    Boolean       = false,
//   nodePath:      Seq[BaseNode] = Seq(),
//   supportsWrite: TransferSizes = TransferSizes.none,
//   supportsRead:  TransferSizes = TransferSizes.none,
//   interleavedId: Option[Int]   = None,
//   device: Option[Device] = None) // The device will not interleave responses (R+B)
// {
//   address.foreach { a => require (a.finite) }
//   address.combinations(2).foreach { case Seq(x,y) => require (!x.overlaps(y), s"$x and $y overlap") }

//   val name = nodePath.lastOption.map(_.lazyModule.name).getOrElse("disconnected")
//   val maxTransfer = max(supportsWrite.max, supportsRead.max)
//   val maxAddress = address.map(_.max).max
//   val minAlignment = address.map(_.alignment).min

//   // The device had better not support a transfer larger than its alignment
//   require (minAlignment >= maxTransfer,
//     s"minAlignment ($minAlignment) must be >= maxTransfer ($maxTransfer)")

//   def toResource: ResourceAddress = {
//     ResourceAddress(address, ResourcePermissions(
//       r = supportsRead,
//       w = supportsWrite,
//       x = executable,
//       c = false,
//       a = false))
//   }
// }

// case class AXI4SlavePortParameters(
//   slaves:     Seq[AXI4SlaveParameters],
//   beatBytes:  Int,
//   minLatency: Int = 1,
//   responseFields: Seq[BundleFieldBase] = Nil,
//   requestKeys:    Seq[BundleKeyBase]   = Nil)
// {
//   require (!slaves.isEmpty)
//   require (isPow2(beatBytes))

//   val maxTransfer = slaves.map(_.maxTransfer).max
//   val maxAddress = slaves.map(_.maxAddress).max

//   // Check the link is not pointlessly wide
//   require (maxTransfer >= beatBytes,
//     s"maxTransfer ($maxTransfer) should not be smaller than bus width ($beatBytes)")
//   // Check that the link can be implemented in AXI4
//   val limit = beatBytes * (1 << AXI4Parameters.lenBits)
//   require (maxTransfer <= limit,
//     s"maxTransfer ($maxTransfer) cannot be larger than $limit on a $beatBytes*8 width bus")

//   // Require disjoint ranges for addresses
//   slaves.combinations(2).foreach { case Seq(x,y) =>
//     x.address.foreach { a => y.address.foreach { b =>
//       require (!a.overlaps(b), s"$a and $b overlap")
//     } }
//   }
// }

// case class AXI4MasterParameters(
//   name:      String,
//   id:        IdRange       = IdRange(0, 1),
//   aligned:   Boolean       = false,
//   maxFlight: Option[Int]   = None, // None = infinite, else is a per-ID cap
//   nodePath:  Seq[BaseNode] = Seq())
// {
//   maxFlight.foreach { m => require (m >= 0) }
// }

// case class AXI4MasterPortParameters(
//   masters:    Seq[AXI4MasterParameters],
//   echoFields:    Seq[BundleFieldBase] = Nil,
//   requestFields: Seq[BundleFieldBase] = Nil,
//   responseKeys:  Seq[BundleKeyBase]   = Nil)
// {
//   val endId = masters.map(_.id.end).max

//   // Require disjoint ranges for ids
//   IdRange.overlaps(masters.map(_.id)).foreach { case (x, y) =>
//     require (!x.overlaps(y), s"AXI4MasterParameters.id $x and $y overlap")
//   }
// }

case class AXI4BundleParameters(
  addrBits:          Int,
  dataBits:          Int,
  idBits:            Int,
  regionPresent:     Boolean,
  wlastPresent:      Boolean,
  rlastPresent:      Boolean,
  sizePresent:       Boolean,
  lenPrenset:        Boolean,
  burstPresent:      Boolean,
  wstrbPresent:      Boolean,
  brespPresent:      Boolean,
  rrespPresent:      Boolean,
  cachePrenset:      Boolean,
  protPrensent:      Boolean,
  qosPrensent:       Boolean,
  exclusiveAccesses: Boolean) {
  require(dataBits >= 8, s"AXI4 data bits must be >= 8 (got $dataBits)")
  require(addrBits >= 1, s"AXI4 addr bits must be >= 1 (got $addrBits)")
  require(idBits >= 1, s"AXI4 id bits must be >= 1 (got $idBits)")
  require(isPow2(dataBits), s"AXI4 data bits must be pow2 (got $dataBits)")

  // Bring the globals into scope
  val regionBits = if (regionPresent) AXI4Parameters.regionBits else 0
  val wlastBits  = if (wlastPresent) AXI4Parameters.wlastBits else 0
  val rlastBits  = if (rlastPresent) AXI4Parameters.rlastBIts else 0
  val sizeBits   = if (sizePresent) AXI4Parameters.sizeBits else 0
  val lenBits    = if (lenPrenset) AXI4Parameters.lenBits else 0
  val burstBits  = if (burstPresent) AXI4Parameters.burstBits else 0
  val wstrBits   = if (wstrbPresent) dataBits / 8 else 0
  val brespBits  = if (brespPresent) AXI4Parameters.respBits else 0
  val rrespBits  = if (rrespPresent) AXI4Parameters.respBits else 0
  val cacheBits  = if (cachePrenset) AXI4Parameters.cacheBits else 0
  val protBits   = if (protPrensent) AXI4Parameters.protBits else 0
  val qosBits    = if (qosPrensent) AXI4Parameters.qosBits else 0
  val lockBits   = if (exclusiveAccesses) AXI4Parameters.lockBits else 0

  def union(x: AXI4BundleParameters) =
    AXI4BundleParameters(
      max(addrBits, x.addrBits),
      max(dataBits, x.dataBits),
      max(idBits, x.idBits),
      regionPresent && x.regionPresent,
      wlastPresent && x.wlastPresent,
      rlastPresent && x.rlastPresent,
      sizePresent && x.sizePresent,
      lenPrenset && x.lenPrenset,
      burstPresent && x.burstPresent,
      wstrbPresent && x.wstrbPresent,
      brespPresent && x.brespPresent,
      rrespPresent && x.rrespPresent,
      cachePrenset && x.cachePrenset,
      protPrensent && x.protPrensent,
      qosPrensent && x.qosPrensent,
      exclusiveAccesses && x.exclusiveAccesses
    )
}

object AXI4BundleParameters {
  val emptyBundleParams = AXI4BundleParameters(
    addrBits          = 1,
    dataBits          = 8,
    idBits            = 0,
    regionPresent     = true,
    wlastPresent      = true,
    rlastPresent      = true,
    sizePresent       = true,
    lenPrenset        = true,
    burstPresent      = true,
    wstrbPresent      = true,
    brespPresent      = true,
    rrespPresent      = true,
    cachePrenset      = true,
    protPrensent      = true,
    qosPrensent       = true,
    exclusiveAccesses = true
  )
  def union(x: Seq[AXI4BundleParameters]) = x.foldLeft(emptyBundleParams)((x, y) => x.union(y))

  // def apply(master: AXI4MasterPortParameters, slave: AXI4SlavePortParameters) =
  //   new AXI4BundleParameters(
  //     addrBits       = log2Up(slave.maxAddress + 1),
  //     dataBits       = slave.beatBytes * 8,
  //     idBits         = log2Up(master.endId),
  //     echoFields     = master.echoFields,
  //     requestFields  = BundleField.accept(master.requestFields, slave.requestKeys),
  //     responseFields = BundleField.accept(slave.responseFields, master.responseKeys)
  //   )
}
