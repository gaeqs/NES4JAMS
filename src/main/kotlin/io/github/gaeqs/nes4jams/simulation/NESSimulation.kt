/*
 *  MIT License
 *
 *  Copyright (c) 2021 Gael Rial Costas
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.gaeqs.nes4jams.simulation

import io.github.gaeqs.nes4jams.audio.NESAPU
import io.github.gaeqs.nes4jams.cpu.NESCPU
import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESAssembledInstruction
import io.github.gaeqs.nes4jams.ppu.NESPPU
import io.github.gaeqs.nes4jams.simulation.event.NESSimulationRenderEvent
import io.github.gaeqs.nes4jams.util.extension.*
import net.jamsimulator.jams.event.SimpleEventBroadcast
import net.jamsimulator.jams.mips.simulation.Simulation
import net.jamsimulator.jams.mips.simulation.event.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.system.measureTimeMillis

class NESSimulation(val data: NESSimulationData) : SimpleEventBroadcast(), Simulation<Short> {

    private val breakpoints = mutableSetOf<Short>()
    private val finishedRunningLock = ReentrantLock()
    private val finishedRunningLockCondition = finishedRunningLock.newCondition()
    private var thread: Thread? = null
    private var interrupted = false
    private var running = false
    private var cycleDelay = 0

    var lastFrameDelayInNanos = 0L
        private set

    @Volatile
    var frame = 0L
        private set

    // region NES

    val cpu = NESCPU(this)
    val ppu = NESPPU(this)
    val apu = NESAPU(this, 9600)
    val controllers = ubyteArrayOf(0u, 0u)
    private val controllersSnapshot = ubyteArrayOf(0u, 0u)
    private val nextControllers = Array(2) { NESControllerMap() }

    var dmaPage: UByte = 0u
    var dmaAddress: UByte = 0u
    var dmaData: UByte = 0u
    var dmaTransfer = false
    var dmaWaitForSync = true

    var clock: Long = 0
        private set
    private var clocksTillCPU: Int = 0

    private val cpuRAM = UByteArray(2048)
    private var lastTick = 0L


    val cartridge get() = data.cartridge

    // endregion

    init {
        cpu.reset()
        cartridge.reset()
        clock = 0
    }

    fun sendNextControllerData(map: NESControllerMap, secondPlayer: Boolean) {
        nextControllers[if (secondPlayer) 1 else 0] = map
    }

    fun cpuWrite(address: UShort, data: UByte) {
        if (cartridge.cpuWrite(address, data)) return
        when (address) {
            in 0x0000u..0x1FFFu -> cpuRAM[address.toInt() and 0x07FF] = data
            in 0x2000u..0x3FFFu -> ppu.cpuWrite(address and 0x0007u, data)
            (0x4014u).toUShort() -> {
                dmaPage = data
                dmaAddress = 0u
                dmaTransfer = true
            }
            (0x4016u).toUShort() -> {
                repeat(controllers.size) { controllersSnapshot[it] = controllers[it] }
            }
            in 0x4000u..0x4013u, (0x4015u).toUShort(), (0x4017u).toUShort() ->
                apu.cpuWrite(address, data)
        }
    }

    fun cpuRead(address: UShort, readOnly: Boolean = false): UByte {
        val (success, data) = cartridge.cpuRead(address)
        if (success) return data
        return when (address) {
            in 0x0000u..0x1FFFu -> cpuRAM[address.toInt() and 0x07FF]
            in 0x2000u..0x3FFFu -> ppu.cpuRead(address and 0x0007u, readOnly)
            (0x4015u).toUShort() -> apu.cpuRead(address)
            in 0x4016u..0x4017u -> {
                val value = controllersSnapshot[(address and 0x1u).toInt()]
                val result: UByte = if (value and 0x80u > 0u) 1u else 0u
                if (!readOnly) {
                    controllersSnapshot[(address and 0x1u).toInt()] = value shl 1
                }
                result or 0x40u
            }
            else -> (address shr 8).toUByte() // Open bus
        }
    }

    fun stealCycles(cycles: Int) {
        clock += cycles
        clocksTillCPU = (clock % 3).toInt()
    }

    fun destroy() {
        stop()
        waitForExecutionFinish()
        apu.destroy()
    }

    private fun velocitySleep() {
        if (cycleDelay > 0) {
            try {
                Thread.sleep(cycleDelay.toLong())
            } catch (ex: InterruptedException) {
                interruptThread()
            }
            if (ppu.frameCompleted) {
                // We don't have to do nothing.
                ppu.frameCompleted = false
                frame++
                callEvent(NESSimulationRenderEvent(this, ppu.screen))
            }
        } else {
            // Run till frame completed
            if (ppu.frameCompleted) {
                ppu.frameCompleted = false
                // Get the delay
                lastFrameDelayInNanos = System.nanoTime() - lastTick

                // Active wait. Thread.sleep() has too much delay.
                val nextTick = lastTick + 1000000000L / (apu.tvType.framerate + 1)
                while (System.nanoTime() < nextTick);
                lastTick = System.nanoTime()
                updateControllers()

                frame++
                callEvent(NESSimulationRenderEvent(this, ppu.screen))
            }
        }
    }

    private fun updateControllers() {
        controllers[0] = nextControllers[0].toByte()
        controllers[1] = nextControllers[1].toByte()
    }

    @Synchronized
    private fun clock() {
        ppu.clock()
        if (clocksTillCPU == 0) {
            apu.clock()
        }

        if (ppu.frameCompleted) {
            apu.onFrameFinish()
        }

        cpu.requestingNMI = ppu.isRequestingNMI()
        cpu.requestingInterrupt = cartridge.mapper.requestingInterrupt || apu.isRequestingInterrupt()

        if (clocksTillCPU == 0) {
            if (dmaTransfer) {
                manageDMA()
            } else {
                cpu.clock()
            }
            clocksTillCPU = 2
        } else clocksTillCPU--

        clock++
    }

    private fun manageDMA() {
        if (dmaWaitForSync) {
            if (clock and 0x1 == 1L) {
                dmaWaitForSync = false
            }
        } else {
            if (clock and 0x1 == 0L) {
                dmaData = cpuRead(dmaPage concatenate dmaAddress)
            } else {
                ppu.cpuWrite(4u, dmaData)
                dmaAddress++
                if (dmaAddress.isZero()) {
                    dmaTransfer = false
                    dmaWaitForSync = true
                }
            }
        }
    }

    override fun getCycleDelay() = cycleDelay
    override fun getCycles() = clock
    override fun isRunning() = running
    override fun getConsole() = data.console
    override fun getBreakpoints() = breakpoints.toSet()


    override fun hasBreakpoint(address: Short) = address in breakpoints

    override fun addBreakpoint(address: Short): Boolean {
        if (breakpoints.add(address)) {
            callEvent(SimulationAddBreakpointEvent(this, address.toInt()))
            return true
        }
        return false
    }

    override fun removeBreakpoint(address: Short): Boolean {
        if (breakpoints.remove(address)) {
            callEvent(SimulationRemoveBreakpointEvent(this, address.toInt()))
            return true
        }
        return false
    }

    override fun toggleBreakpoint(address: Short) {
        if (address in breakpoints) {
            breakpoints -= address
            callEvent(SimulationRemoveBreakpointEvent(this, address.toInt()))
        } else {
            breakpoints += address
            callEvent(SimulationAddBreakpointEvent(this, address.toInt()))
        }
    }

    override fun setCycleDelay(delay: Int) {
        cycleDelay = max(0, cycleDelay)
    }

    override fun addCycleCount() {
        clock++
    }

    override fun interruptThread() {
        interrupted = true
    }

    override fun checkThreadInterrupted(): Boolean {
        if (Thread.interrupted()) interrupted = true
        return interrupted
    }

    override fun stop() {
        thread?.interrupt()
        thread = null
        runSynchronized { interrupted = true }
    }

    override fun reset() {
        stop()
        waitForExecutionFinish()

        clock = 0L

        // Reset controllers
        controllers.fill(0u)
        controllersSnapshot.fill(0u)
        nextControllers.fill(NESControllerMap())

        // Reset RAM
        cpuRAM.fill(0u)

        // Reset DMA
        dmaAddress = 0u
        dmaData = 0u
        dmaPage = 0u
        dmaTransfer = false
        dmaWaitForSync = true

        cpu.reset()
        ppu.reset()
        apu.reset()
        cartridge.reset()
        clock = 0
    }

    override fun waitForExecutionFinish() {
        finishedRunningLock.withLock {
            try {
                if (running) {
                    finishedRunningLockCondition.await()
                }
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
            }
        }
    }

    override fun executeOneStep() {
        if (running) return
        running = true
        interrupted = false

        thread = Thread {
            try {
                apu.resume()
                val before = callEvent(SimulationCycleEvent.Before(this, cycles))
                if (before.isCancelled) return@Thread
                clock()
                callEvent(SimulationCycleEvent.After(this, cycles))
                manageSimulationFinish()
                apu.pause()
            } catch (ex: Exception) {
                manageException(ex)
            }
        }.apply { priority = Thread.MAX_PRIORITY; name = "NES Simulation (${cartridge.file.name})"; isDaemon = true }
        callEvent(SimulationStartEvent(this))
        thread?.start()
    }

    override fun executeAll() {
        if (running) return
        running = true
        interrupted = false
        thread = Thread {
            try {
                apu.resume()
                val clockStart = clock
                val millis = measureTimeMillis {
                    lastTick = System.nanoTime()
                    if (data.callEvents) executeAllWithEvents()
                    else executeAllWithoutEvents()
                }
                apu.pause()

                console?.apply {
                    println()
                    printInfoLn("${clock - clockStart} cycles executed in $millis millis.")
                    printInfoLn("${((clock - clockStart) / (millis / 1000.0)).toInt()} cycles/s")
                    println()
                }

                manageSimulationFinish()
            } catch (ex: Exception) {
                manageException(ex)
            }
        }.apply { priority = Thread.MAX_PRIORITY; name = "NES Simulation (${cartridge.file.name})"; isDaemon = true }
        callEvent(SimulationStartEvent(this))
        thread?.start()
    }

    private fun executeAllWithEvents() {
        val before = callEvent(SimulationCycleEvent.Before(this, cycles))
        if (!before.isCancelled) {
            clock()
            callEvent(SimulationCycleEvent.After(this, cycles))
        }
        while (!checkThreadInterrupted()) {
            velocitySleep()
            if (!checkThreadInterrupted()) {
                val before = callEvent(SimulationCycleEvent.Before(this, cycles))
                if (!before.isCancelled) {
                    clock()
                    callEvent(SimulationCycleEvent.After(this, cycles))
                }
            }
        }
    }

    private fun executeAllWithoutEvents() {
        clock()
        while (!checkThreadInterrupted()) {
            velocitySleep()
            if (!checkThreadInterrupted()) {
                clock()
            }
        }
    }

    private fun manageSimulationFinish() {
        finishedRunningLock.withLock {
            running = false
            finishedRunningLockCondition.signalAll()
            callEvent(SimulationStopEvent(this))
            console?.flush()
        }
    }

    override fun isUndoEnabled() = data.undoEnabled

    override fun undoLastStep(): Boolean {
        TODO("Not yet implemented")
    }

    @Synchronized
    override fun runSynchronized(runnable: Runnable) = runnable.run()


    private fun manageException(ex: Exception) {
        manageSimulationFinish()

        System.err.println("------------------------------------")
        System.err.println("Simulation exception:")
        System.err.println("Cycle:               $clock")
        System.err.println("PC:                  ${cpu.pc}")
        System.err.println(
            "Current instruction: $${cpu.currentOpcode.toHex(2).uppercase()} " +
                    "(${NESAssembledInstruction.INSTRUCTIONS[cpu.currentOpcode.toInt()].mnemonic})"
        )
        System.err.println("PPU cycle:           ${ppu.cycle}")
        System.err.println("PPU scanline:        ${ppu.scanline}")
        System.err.println("Exception:")
        ex.printStackTrace()
        System.err.println("------------------------------------")

    }

    fun disassembleCurrentStatus(from: UShort, to: UShort): Map<Int, String> {
        var address = from
        var value: UByte
        var low: UByte
        var high: UByte
        val map = HashMap<Int, String>()
        var lineStart = address.toInt()

        while (address <= to) {
            if (lineStart > address.toInt()) break
            lineStart = address.toInt()
            var string = "$${address.toString(16)}:  "
            val opcode = cpuRead(address++, true)
            val instruction = NESAssembledInstruction.INSTRUCTIONS[opcode.toInt()]
            string += "${instruction.mnemonic}  "

            string += when (instruction.addressingMode) {
                NESAddressingMode.IMPLIED -> "{IMP}"
                NESAddressingMode.IMMEDIATE -> {
                    value = cpuRead(address++, true)
                    "#$${value.toHex(2)} {IMM}"
                }
                NESAddressingMode.ZERO_PAGE -> {
                    low = cpuRead(address++, true)
                    "$${low.toHex(2)}, {ZP0}"
                }
                NESAddressingMode.ZERO_PAGE_X -> {
                    low = cpuRead(address++, true)
                    "$${low.toHex(2)}, X {ZPX}"
                }
                NESAddressingMode.ZERO_PAGE_Y -> {
                    low = cpuRead(address++, true)
                    "$${low.toHex(2)}, Y {ZPY}"
                }
                NESAddressingMode.INDIRECT_X -> {
                    low = cpuRead(address++, true)
                    "($${low.toHex(2)}, X) {IZX}"
                }
                NESAddressingMode.INDIRECT_Y -> {
                    low = cpuRead(address++, true)
                    "($${low.toHex(2)}), Y {IZY}"
                }
                NESAddressingMode.ABSOLUTE -> {
                    low = cpuRead(address++, true)
                    high = cpuRead(address++, true)
                    "$${(high concatenate low).toHex(4)} {ABS}"
                }
                NESAddressingMode.ABSOLUTE_X -> {
                    low = cpuRead(address++, true)
                    high = cpuRead(address++, true)
                    "$${(high concatenate low).toHex(4)}, X {ABX}"
                }
                NESAddressingMode.ABSOLUTE_Y -> {
                    low = cpuRead(address++, true)
                    high = cpuRead(address++, true)
                    "$${(high concatenate low).toHex(4)}, Y {ABY}"
                }
                NESAddressingMode.INDIRECT -> {
                    low = cpuRead(address++, true)
                    high = cpuRead(address++, true)
                    "($${(high concatenate low).toHex(4)}) {IND}"
                }
                NESAddressingMode.RELATIVE -> {
                    value = cpuRead(address++, true)
                    "$${value.toHex(2)} [$${(address.toInt() + value.toByte()).toString(16)}] {REL}"
                }
            }
            map[lineStart] = string
        }
        return map
    }
}