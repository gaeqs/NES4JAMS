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

import com.github.strikerx3.jxinput.XInputDevice
import com.github.strikerx3.jxinput.enums.XInputButton
import com.github.strikerx3.jxinput.listener.XInputDeviceListener
import javafx.scene.input.KeyCode
import net.jamsimulator.jams.manager.ResourceProvider

class NESXInputController(
    mapping: Map<String, NESButton>,
    deviceId: Int
) : NESControllerDevice(mapping), XInputDeviceListener {

    private val device = run {
        return@run try {
            if (XInputDevice.isAvailable()) XInputDevice.getDeviceFor(deviceId).apply {
                addListener(this@NESXInputController)
            } else null
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private var _currentState = NESControllerMap()
    override val currentState: NESControllerMap
        get() {
            device?.poll()
            return _currentState
        }

    override fun connected() {
        _currentState = NESControllerMap()
    }

    override fun disconnected() {
        _currentState = NESControllerMap()
    }

    override fun buttonChanged(key: XInputButton, pressed: Boolean) {
        val button = mapping[key.name] ?: return
        _currentState = currentState.with(button, pressed)
    }

    class Builder(resourceProvider: ResourceProvider) : NESControllerDeviceBuilder(NAME, resourceProvider) {

        companion object {
            const val NAME = "X_INPUT"
            const val DEVICE_NODE = "device"
        }

        override val mappingKeys: List<String> get() = XInputButton.values().map { it.name }

        override fun build(mapping: Map<String, NESButton>, extra: Map<String, String>): NESControllerDevice {
            val device = extra[DEVICE_NODE]?.toIntOrNull() ?: 0
            return NESXInputController(mapping, device)
        }

    }
}