/*
 *  MIT License
 *
 *  Copyright (c) 2022 Gael Rial Costas
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

package io.github.gaeqs.nes4jams.audio

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BeeperThread(val apu: NESAPU, sampleRate: Int) : Thread() {

    private val pauseLock = ReentrantLock()
    private val pauseCondition = pauseLock.newCondition()

    private val beeper = Beeper(sampleRate)

    private var running = false

    private var paused = true

    override fun run() {
        running = true
        while (running) {
            if (paused) pauseLock.withLock { pauseCondition.await() }
            beeper.sample(apu.fetchSample())
        }
    }

    fun play() {
        pauseLock.withLock {
            paused = false
            pauseCondition.signalAll()
        }
    }

    fun pause() {
        pauseLock.withLock {
            paused = true
        }
    }

    fun kill() {
        running = false
        beeper.destroy()
    }

    fun reset() {
        beeper.reset()
    }

}