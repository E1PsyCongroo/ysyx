package rvcpu.dev

import chisel3._
import chisel3.util._

case class Area(start: BigInt, end: BigInt)

object Dev {
  val memoryAddr        = Area(BigInt("80000000", 16), BigInt("87ffffff", 16))
  val uartAddr          = Area(BigInt("a00003f8", 16), BigInt("a00003f8", 16))
  val mtimeAddr         = Area(BigInt("a0000048", 16), BigInt("a000004d", 16))
  val CLINTAddr	        = Area(BigInt("02000000", 16), BigInt("0200ffff", 16))
  val SRAMAddr          = Area(BigInt("0f000000", 16), BigInt("0fffffff", 16))
  val UART16550Addr     = Area(BigInt("10000000", 16), BigInt("10000fff", 16))
  val SPIMasterAddr     = Area(BigInt("10001000", 16), BigInt("10001fff", 16))
  val GPIOAddr          = Area(BigInt("10002000", 16), BigInt("1000200f", 16))
  val PS2Addr           = Area(BigInt("10011000", 16), BigInt("10011007", 16))
  val MROMAddr          = Area(BigInt("20000000", 16), BigInt("20000fff", 16))
  val VGAAddr           = Area(BigInt("21000000", 16), BigInt("211fffff", 16))
  val FlashAddr         = Area(BigInt("30000000", 16), BigInt("3fffffff", 16))
  val ChipLinkMMIOAddr  = Area(BigInt("40000000", 16), BigInt("7fffffff", 16))
  val PSRAMAddr         = Area(BigInt("80000000", 16), BigInt("9fffffff", 16))
  val SDRAMAddr         = Area(BigInt("a0000000", 16), BigInt("bfffffff", 16))
  val ChipLinkMEMAddr   = Area(BigInt("c0000000", 16), BigInt("ffffffff", 16))
}
