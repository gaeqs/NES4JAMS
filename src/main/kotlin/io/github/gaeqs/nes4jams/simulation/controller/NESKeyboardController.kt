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

package io.github.gaeqs.nes4jams.simulation.controller

import io.github.gaeqs.nes4jams.data.NES4JAMS_CONTROLLER_KEYBOARD
import javafx.scene.input.KeyCode
import net.jamsimulator.jams.manager.ResourceProvider

class NESKeyboardController(mapping: Map<String, NESButton>) : NESControllerDevice(mapping) {

    @Volatile
    override var currentState = NESControllerMap()

    override fun updateKeyboardKey(key: KeyCode, pressed: Boolean) {
        val button = mapping[key.name] ?: return
        currentState = currentState.with(button, pressed)
    }

    override fun dispose() {
        // Nothing to dispose
    }

    class Builder(resourceProvider: ResourceProvider) :
        NESControllerDeviceBuilder(
            NAME,
            resourceProvider,
            NES4JAMS_CONTROLLER_KEYBOARD,
            DEFAULT_MAPPER,
            DEFAULT_EXTRA
        ) {

        companion object {
            const val NAME = "KEYBOARD"
            val DEFAULT_MAPPER = mapOf(
                "X" to NESButton.A,
                "Z" to NESButton.B,
                "S" to NESButton.SELECT,
                "A" to NESButton.START,
                "UP" to NESButton.UP,
                "DOWN" to NESButton.DOWN,
                "LEFT" to NESButton.LEFT,
                "RIGHT" to NESButton.RIGHT,
            )
            val DEFAULT_EXTRA = emptyMap<String, String>()
        }

        override val mappingKeys: List<String> get() = KeyCode.values().map { it.name }

        override val defaultExtraTypes = emptyMap<String, String>()

        override fun build(mapping: Map<String, NESButton>, extra: Map<String, String>): NESControllerDevice {
            return NESKeyboardController(mapping)
        }
    }

}