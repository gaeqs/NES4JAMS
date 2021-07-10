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

import net.jamsimulator.jams.configuration.Configuration
import net.jamsimulator.jams.gui.util.converter.ValueConverters
import net.jamsimulator.jams.utils.Validate
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfigurationNodePreset as Preset

class NESSimulationConfiguration {

    var name: String
        set(value) {
            Validate.isTrue(value.isNotEmpty(), "Name cannot be empty!")
            field = value
        }

    val nodes = hashMapOf<Preset, Any>()
    val rawValues = hashMapOf<String, Any>()

    constructor(name: String) {
        this.name = name
        Preset.PRESETS.forEach { nodes[it] = it.defaultValue }
    }

    constructor(name: String, configuration: NESSimulationConfiguration) {
        this.name = name
        nodes += configuration.nodes
    }

    constructor(name: String, configuration: Configuration) {
        this.name = name


        val nodesConfig = configuration.getOrCreateConfiguration("node")
        rawValues += nodesConfig.getAll(false).toMutableMap()

        Preset.PRESETS.forEach {
            val optional = nodesConfig.getAndConvert<Any>(it.name, it.type.java)
            if (optional.isEmpty) {
                nodes[it] = it.defaultValue
            } else {
                rawValues -= it.name
                nodes[it] = optional.get()
            }
        }
    }

    fun <T> getNodeValue(node: String): T? {
        try {
            val preset = Preset.PRESETS.find { it.name == node } ?: return rawValues[node] as T?

            // Presets may change! Let's check the raw values
            if (node in rawValues) {
                var value = rawValues.remove(node) ?: preset.defaultValue
                if (!preset.type.isInstance(value)) {
                    val optional = ValueConverters.getByType(preset.type.java)
                    value = if (optional.isPresent) {
                        optional.get().fromString(value.toString()) ?: preset.defaultValue
                    } else {
                        preset.defaultValue
                    }
                }
                nodes[preset] = value
            }

            return nodes.getOrDefault(preset, preset.defaultValue) as T?
        } catch (ex: ClassCastException) {
            ex.printStackTrace()
            return null
        }
    }

    fun setNodeValue(node: String, value: Any): Boolean {
        val preset = Preset.PRESETS.find { it.name == node }
        if (preset == null) {
            rawValues[node] = value
        } else {
            if (!preset.type.isInstance(value)) return false
            nodes[preset] = value
        }
        return true
    }

    fun save(configuration: Configuration, prefix: String) {
        val usingPrefix = "$prefix.$name.node."
        rawValues.forEach { (key, value) -> configuration.set(usingPrefix + key, value) }
        nodes.forEach { (key, value) -> configuration.convertAndSet(usingPrefix + key.name, value, key.type.java) }
    }

    override fun toString(): String {
        return "NESSimulationConfiguration(name='$name', nodes=$nodes, rawValues=$rawValues)"
    }

}