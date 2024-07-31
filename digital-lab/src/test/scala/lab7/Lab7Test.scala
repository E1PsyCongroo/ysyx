import org.scalatest._

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PS2Control extends Module {
  val io = IO(new Bundle{
    val ps2Clk = Input(UInt(1.W))
    val ps2Data = Input(UInt(1.W))
    val seg0 = Output(UInt(8.W))
    val seg1 = Output(UInt(8.W))
    val seg4 = Output(UInt(8.W))
    val seg5 = Output(UInt(8.W))
    val ready = Output(Bool())
    val overflow = Output(Bool())
  })
  val nextdata_n = RegInit(true.B)
  val keyboard = Module(new ps2Keyboard)
  keyboard.io.ps2Clk := io.ps2Clk
  keyboard.io.ps2Data := io.ps2Data
  keyboard.io.nextdata_n := nextdata_n
  val ready = keyboard.io.ready
  val data = keyboard.io.data

  object State extends ChiselEnum {
    val idle, pressed, release = Value
  }

  import State._
  val state = RegInit(idle)
  val readFlag = state === idle && ready
  val codeReg = RegEnable(data, readFlag)

  val count = RegInit(0.U(8.W))
  count := Mux(readFlag, count + 1.U, count)

  state := MuxLookup(state, idle)(Seq(
    idle -> Mux(ready, pressed, idle),
    pressed -> Mux(ready && data === "hF0".U, release, pressed),
    release -> Mux(ready && data === codeReg, idle, release)
  ))

  nextdata_n := ~ready && nextdata_n

  val sevSegDecoder = Seq.fill(4)(Module(new SevenSegDecoder))
  sevSegDecoder(0).io.sw := Mux(state === pressed, codeReg(3, 0), 0.U)
  sevSegDecoder(1).io.sw := Mux(state === pressed, codeReg(7, 4), 0.U)
  sevSegDecoder(2).io.sw := count(3, 0)
  sevSegDecoder(3).io.sw := count(7, 4)

  io.overflow := keyboard.io.overflow
  io.ready := ready
  io.seg0 := sevSegDecoder(0).io.seg
  io.seg1 := sevSegDecoder(1).io.seg
  io.seg4 := sevSegDecoder(2).io.seg
  io.seg5 := sevSegDecoder(3).io.seg
}


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

  behavior of "ps2Control"
  it should "generate vcd" in {
    test(new PS2Control).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
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
      dut.clock.step(20)
      kbdSendCode(0x1C) // press 'A'
      dut.clock.step(20)
      kbdSendCode(0xF0) // break code
      dut.clock.step(20)
      kbdSendCode(0x1C) // release 'A'
      dut.clock.step(20)
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

