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

package io.github.gaeqs.nes4jams.gui.simulation

import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.simulation.event.NESSimulationRenderEvent
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.util.Duration
import net.jamsimulator.jams.event.Listener

class NESSimulationInfo(private val simulation: NESSimulation) : HBox() {

    private val label = Label("FPS: ")
    private val timeline = Timeline(KeyFrame(Duration.seconds(0.5), {
        updateFPS()
    }))


    init {
        children += label
        simulation.registerListeners(this, true)

        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()
    }

    fun dispose () {
        timeline.stop()
    }

    private fun updateFPS() {
        val delay = simulation.lastFrameDelayInNanos
        val delayInSeconds = delay / 1_000_000_000.0
        val fps = 1 / delayInSeconds
        label.text = "FPS: %.2f".format(fps)
    }

}