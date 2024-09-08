package rvcpu.dev

import chisel3._
import chisel3.util._

case class Area(start: BigInt, end: BigInt)

object Dev {
  val memoryAddr  = Area(BigInt("80000000", 16), BigInt("87ffffff", 16))
  val uartAddr    = Area(BigInt("a00003f8", 16), BigInt("a00003f8", 16))
  val mtimeAddr   = Area(BigInt("a0000048", 16), BigInt("a000004d", 16))
}
