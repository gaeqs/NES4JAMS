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
import net.jamsimulator.jams.project.FilesToAssemble
import net.jamsimulator.jams.project.FilesToAssemblerHolder
import net.jamsimulator.jams.project.ProjectData
import java.util.*

class NESProjectData(project: NESProject) :
    ProjectData(NESProjectType.INSTANCE, project.folder), FilesToAssemblerHolder {

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
        val configuration = configurations.find { it.name == name } ?: return false
        val before = callEvent(NESSimulationConfigurationRemoveEvent.Before(this, configuration))
        if (before.isCancelled) return false
        val result = _configurations.remove(configuration)
        if (result) {
            callEvent(NESSimulationConfigurationRemoveEvent.After(this, configuration))
            if (selectedConfiguration == configuration) selectConfiguration(configurations.find { true }?.name)
        }
        return result
    }

    fun selectConfiguration(name: String?) {
    }

    override fun getFilesToAssemble(): FilesToAssemble = filesToAssemble

    override fun load() {
        if (loaded) return
        super.load()
        filesToAssemble.load(metadataFolder)
    }

    override fun save() {
        filesToAssemble.save(metadataFolder)
        super.save()
    }
}