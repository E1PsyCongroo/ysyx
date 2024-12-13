package rvcpu.dev

import chisel3._
import chisel3.util._

object Dev {

  val ALL = AddressSet.everything

  object npc {
    val PMEMAddr     = AddressSet(BigInt("80000000", 16), BigInt("07ffffff", 16))
    val UARTAddr     = AddressSet(BigInt("a00003f8", 16), BigInt("00000000", 16))
    val CLINTAddr    = AddressSet(BigInt("a0000048", 16), BigInt("00000007", 16))
    val KDBAddr      = AddressSet(BigInt("a0000060", 16), BigInt("00000003", 16))
    val VGACTLAddr   = AddressSet(BigInt("a0000100", 16), BigInt("00000007", 16))
    val VGABUFAddr   = AddressSet(BigInt("a1000000", 16), BigInt("001fffff", 16))
    val AUDIOCTLAddr = AddressSet(BigInt("a0000200", 16), BigInt("0000001f", 16))
    val AUDIOBUFAddr = AddressSet(BigInt("a2000000", 16), BigInt("0000ffff", 16))
  }

  object ysyxsoc {
    val CLINTAddr        = AddressSet(BigInt("02000000", 16), BigInt("0000ffff", 16))
    val SRAMAddr         = AddressSet(BigInt("0f000000", 16), BigInt("00ffffff", 16))
    val UART16550Addr    = AddressSet(BigInt("10000000", 16), BigInt("00000fff", 16))
    val SPIMasterAddr    = AddressSet(BigInt("10001000", 16), BigInt("00000fff", 16))
    val GPIOAddr         = AddressSet(BigInt("10002000", 16), BigInt("0000000f", 16))
    val PS2Addr          = AddressSet(BigInt("10011000", 16), BigInt("00000007", 16))
    val MROMAddr         = AddressSet(BigInt("20000000", 16), BigInt("00000fff", 16))
    val VGAAddr          = AddressSet(BigInt("21000000", 16), BigInt("001fffff", 16))
    val FlashAddr        = AddressSet(BigInt("30000000", 16), BigInt("0fffffff", 16))
    val ChipLinkMMIOAddr = AddressSet(BigInt("40000000", 16), BigInt("3fffffff", 16))
    val PSRAMAddr        = AddressSet(BigInt("80000000", 16), BigInt("1fffffff", 16))
    val SDRAMAddr        = AddressSet(BigInt("a0000000", 16), BigInt("1fffffff", 16))
    val ChipLinkMEMAddr  = AddressSet(BigInt("c0000000", 16), BigInt("3fffffff", 16))
  }

  object fpga {
    val CLINTAddr = AddressSet(BigInt("02000000", 16), BigInt("00000fff", 16))
    val BRAMAddr  = AddressSet(BigInt("0f000000", 16), BigInt("0003ffff", 16))
    val UARTAddr  = AddressSet(BigInt("10000000", 16), BigInt("00000fff", 16))
    val GPIOAddr  = AddressSet(BigInt("10002000", 16), BigInt("00000fff", 16))
    val VGAAddr   = AddressSet(BigInt("21000000", 16), BigInt("001fffff", 16))
  }
}
