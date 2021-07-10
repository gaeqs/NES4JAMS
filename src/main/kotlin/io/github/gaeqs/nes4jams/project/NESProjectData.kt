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

package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationAddEvent
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationRemoveEvent
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationSelectEvent
import io.github.gaeqs.nes4jams.utils.extension.orNull
import net.jamsimulator.jams.configuration.Configuration
import net.jamsimulator.jams.project.FilesToAssemble
import net.jamsimulator.jams.project.FilesToAssemblerHolder
import net.jamsimulator.jams.project.ProjectData
import java.util.*


class NESProjectData(project: NESProject) :
    ProjectData(NESProjectType.INSTANCE, project.folder), FilesToAssemblerHolder {

    companion object {
        const val NODE_CONFIGURATIONS = "nes.configurations"
        const val NODE_SELECTED_CONFIGURATION = "nes.selected_configuration"
    }

    val filesToAssemble = NESFilesToAssemble(project)
    var selectedConfiguration: NESSimulationConfiguration? = null
        private set

    private val _configurations = mutableSetOf<NESSimulationConfiguration>()

    val configurations: Set<NESSimulationConfiguration> = Collections.unmodifiableSet(_configurations)

    fun addConfiguration(configuration: NESSimulationConfiguration): Boolean {
        if (_configurations.any { it.name == configuration.name }) return false
        val before = callEvent(NESSimulationConfigurationAddEvent.Before(this, configuration))
        if (before.isCancelled) return false
        val result = _configurations.add(configuration)
        if (result) {
            callEvent(NESSimulationConfigurationAddEvent.After(this, configuration))
            if (selectedConfiguration == null) selectConfiguration(configuration.name)
        }
        return result
    }

    fun removeConfiguration(name: String): Boolean {
        val configuration = _configurations.find { it.name == name } ?: return false
        val before = callEvent(NESSimulationConfigurationRemoveEvent.Before(this, configuration))
        if (before.isCancelled) return false
        val result = _configurations.remove(configuration)
        if (result) {
            callEvent(NESSimulationConfigurationRemoveEvent.After(this, configuration))
            if (selectedConfiguration == configuration) selectConfiguration(_configurations.find { true }?.name)
        }
        return result
    }

    fun selectConfiguration(name: String?): Boolean {
        val configuration = if (name == null) null else _configurations.find { it.name == name }
        if (configuration == null && name != null || configuration == selectedConfiguration) return false
        val old = selectedConfiguration
        if (callEvent(NESSimulationConfigurationSelectEvent.Before(this, old, configuration)).isCancelled)
            return false
        selectedConfiguration = configuration
        callEvent(NESSimulationConfigurationSelectEvent.After(this, old, configuration))
        return true
    }

    override fun getFilesToAssemble(): FilesToAssemble = filesToAssemble

    override fun load() {
        if (loaded) return
        super.load()
        filesToAssemble.load(metadataFolder)

        data.get<Configuration>(NODE_CONFIGURATIONS).ifPresent {
            it.getAll(false).forEach { (name, data) ->
                if (data is Configuration) {
                    _configurations += NESSimulationConfiguration(name, data)
                }
            }
        }

        val selectedConfig = data.getString(NODE_SELECTED_CONFIGURATION).orNull()
        selectedConfiguration = configurations.find { it.name == selectedConfig }

        // Let's try to get a default configuration
        if(selectedConfiguration == null) {
            selectedConfiguration = configurations.find { true }
        }
    }

    override fun save() {
        filesToAssemble.save(metadataFolder)
        _configurations.forEach { it.save(data, NODE_CONFIGURATIONS) }
        data.set(NODE_SELECTED_CONFIGURATION, selectedConfiguration?.name)
        super.save()
    }
}