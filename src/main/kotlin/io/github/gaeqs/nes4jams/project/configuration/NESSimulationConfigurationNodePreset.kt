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

package io.github.gaeqs.nes4jams.project.configuration

import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.utils.Validate
import kotlin.reflect.KClass

data class NESSimulationConfigurationNodePreset(
    val name: String,
    val type: KClass<out Any>,
    val priority: Int,
    val languageNode: String,
    val defaultValue: Any,
    val dependencies: Map<String, Array<Any>> = hashMapOf()
) {

    init {
        Validate.isTrue(
            type.isInstance(defaultValue), "Type doesn't match default value! "
                    + defaultValue
                    + " (" + defaultValue.javaClass + ")"
                    + " -> " + type
        );
    }

    fun supportsNode(node: String, state: Any): Boolean {
        return dependencies.isEmpty() || state in (dependencies[node] ?: return true)
    }

    companion object {
        const val CALL_EVENTS = "call_events"
        const val UNDO_ENABLED = "undo_enabled"

        val PRESETS = hashSetOf(
            NESSimulationConfigurationNodePreset(
                CALL_EVENTS,
                Boolean::class,
                90,
                Messages.SIMULATION_CONFIGURATION_CALL_EVENTS,
                true
            ),
            NESSimulationConfigurationNodePreset(
                UNDO_ENABLED,
                Boolean::class,
                89,
                Messages.SIMULATION_CONFIGURATION_ENABLE_UNDO,
                true,
                mapOf(CALL_EVENTS to arrayOf(true))
            )
        )
    }

}