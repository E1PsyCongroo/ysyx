import org.scalatest._

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Lab7Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ps2Keyboard"
  it should "generate vcd" in {
    test(new ps2Keyboard).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val kbdCLKPeriod = 60

      def kbdSendCode(code: Int): Unit = {
        val codeBits = code.toBinaryString.reverse.padTo(8, '0').reverse
        val parityBit = if (codeBits.count(_ == '1') % 2 == 0) 1 else 0
        val sendBuffer = Seq(0) ++ codeBits.map(_.asDigit) ++ Seq(parityBit, 1) // start bit, data bits, parity bit, stop bit

        println(sendBuffer)

        for (i <- sendBuffer.indices) {
          dut.io.ps2Data.poke(sendBuffer(i).U)
          dut.clock.step(kbdCLKPeriod / 2)
          dut.io.ps2Clk.poke(0.U)
          dut.clock.step(kbdCLKPeriod / 2)
          dut.io.ps2Clk.poke(1.U)
        }
      }
      dut.reset.poke(true.B)
      dut.clock.step(20)
      dut.reset.poke(false.B)
      dut.clock.step(20)
      dut.io.ps2Clk.poke(1.U)
      dut.io.ps2Data.poke(1.U)
      dut.io.nextdata_n.poke(true.B)
      dut.clock.step(20)
      kbdSendCode(0x1C) // press 'A'
      dut.clock.step(20)
      dut.io.nextdata_n.poke(false.B)
      dut.clock.step(20)
      dut.io.nextdata_n.poke(true.B)
      kbdSendCode(0xF0) // break code
      dut.clock.step(20)
      dut.io.nextdata_n.poke(false.B)
      dut.clock.step(20)
      dut.io.nextdata_n.poke(true.B)
      kbdSendCode(0x1C) // release 'A'
      dut.clock.step(20)
      dut.io.nextdata_n.poke(false.B)
      dut.clock.step(20)
      dut.io.nextdata_n.poke(true.B)
      kbdSendCode(0x1B) // press 'S'
      dut.clock.step(20)
      kbdSendCode(0x1B) // keep pressing 'S'
      dut.clock.step(20)
      kbdSendCode(0x1B) // keep pressing 'S'
      kbdSendCode(0xF0) // break code
      kbdSendCode(0x1B) // release 'S'
      dut.clock.step(20)
    }
  }
}

