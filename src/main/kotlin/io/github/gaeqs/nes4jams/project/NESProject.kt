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

import io.github.gaeqs.nes4jams.cpu.assembler.NESAssembler
import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerMemoryBank
import io.github.gaeqs.nes4jams.gui.project.NESStructurePane
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfigurationNodePreset
import javafx.scene.control.Tab
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.project.BasicProject
import net.jamsimulator.jams.project.ProjectType
import net.jamsimulator.jams.utils.RawFileData
import java.io.File
import java.io.OutputStream
import kotlin.system.measureTimeMillis

class NESProject(folder: File) : BasicProject(folder, true) {

    fun assembleToFile(log: Log?) {
        val configuration = getData().selectedConfiguration
        if (configuration == null) {
            log?.printErrorLn("Error! Configuration not found!")
            throw AssemblerException("Configuration not found.")
        }

        log?.printInfoLn("Assembling NES project ${data.name} using configuration ${configuration.name}")
        log?.println()
        log?.printInfoLn("Files:")

        val rootPath = folder.toPath()
        val files = getData().filesToAssemble.files.map {
            try {
                log?.printInfoLn("- ${it.absolutePath}")
                RawFileData(it, rootPath)
            } catch (ex: Exception) {
                throw AssemblerException("Error while loading file $it.", ex)
            }
        }

        val millis = measureTimeMillis {
            val assembler = NESAssembler(
                files,
                configuration.getNodeValue(NESSimulationConfigurationNodePreset.MEMORY_BANKS)!!,
                log
            )

            log?.println()
            log?.printInfoLn("Assembling...")
            assembler.assemble()
            log?.printDoneLn("Done! Writing to file...")

            val header = configuration.generateCartridgeHeader()

            val out = File(data.filesFolder, "$name.nes").outputStream()
            val (banksAmount, extra) = calculateProgramBanks(assembler.banks)
            header.setPrgRomBanks(banksAmount.toUShort())

            out.write(header.toByteArray())
            writeProgram(out, assembler.banks, extra)
            //out.write(chrMemory)
            out.close()
        }

        log?.printDoneLn("Assembly successful in $millis ms.")
        return
    }

    private fun calculateProgramBanks(banks: Iterable<NESAssemblerMemoryBank>): Pair<Int, Int> {
        val total = banks.filter { it.writeOnCartridge }.sumOf { it.array.size }
        val banksAmount = total shr 14
        val extra = total - (banksAmount shl 14)
        return if (extra > 0) Pair(banksAmount + 1, extra) else Pair(banksAmount, 0)
    }

    private fun writeProgram(
        out: OutputStream,
        banks: Iterable<NESAssemblerMemoryBank>,
        extra: Int
    ) {
        var total = 0
        banks.filter { it.writeOnCartridge }.forEach {
            out.write(it.array.toByteArray())
            total += it.array.size
        }

        if (extra > 0) {
            repeat(0x4000 - extra) { out.write(0) }
        }
    }

    override fun generateSimulation(log: Log?) {
        assembleToFile(log)
    }

    override fun onClose() {
        data.save()
        if (projectTab != null) {
            val pane = projectTab.projectTabPane.workingPane
            if (pane is NESStructurePane) {
                pane.holder.closeAll(true)
            }
        }
    }

    override fun generateMainProjectPane(tab: Tab, projectTab: ProjectTab): WorkingPane =
        NESStructurePane(tab, projectTab, this)

    override fun loadData() {
        data = NESProjectData(this)
        data.load()
    }

    override fun getType(): ProjectType<*> {
        return NESProjectType.INSTANCE
    }

    override fun getData(): NESProjectData {
        return super.getData() as NESProjectData
    }
}