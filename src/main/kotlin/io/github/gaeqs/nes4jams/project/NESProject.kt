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

import io.github.gaeqs.nes4jams.cartridge.Cartridge
import io.github.gaeqs.nes4jams.cartridge.CartridgeHeader
import io.github.gaeqs.nes4jams.cpu.assembler.NESAssembler
import io.github.gaeqs.nes4jams.cpu.assembler.NESAssemblerMemoryBank
import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import io.github.gaeqs.nes4jams.gui.project.NESSimulationPane
import io.github.gaeqs.nes4jams.gui.project.NESStructurePane
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfigurationNodePreset
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.simulation.NESSimulationData
import io.github.gaeqs.nes4jams.util.ExponentialPrgBanksFinder
import javafx.application.Platform
import javafx.scene.control.Tab
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.log.Console
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.project.BasicProject
import net.jamsimulator.jams.project.ProjectType
import net.jamsimulator.jams.utils.RawFileData
import java.io.File
import java.io.OutputStream
import kotlin.system.measureTimeMillis

class NESProject(folder: File) : BasicProject(folder, true) {

    // region assemble

    fun assembleToFile(log: Log?): Triple<NESSimulationConfiguration, NESAssembler, File> {
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

        val file = File(data.filesFolder, "$name.nes")
        val assembler = NESAssembler(
            files,
            configuration.getNodeValue(NESSimulationConfigurationNodePreset.MEMORY_BANKS)!!,
            log
        )

        val millis = measureTimeMillis {


            log?.println()
            log?.printInfoLn("Assembling...")
            assembler.assemble()

            log?.printInfoLn("Loading CHR data...")

            val pcxFiles = getData().spritesToAssemble.files.map {
                val stream = it.inputStream()
                val pcx = PictureExchangeImage(stream)
                stream.close()
                pcx
            }

            log?.printInfoLn("Writing to file...")
            val header = configuration.generateCartridgeHeader()
            val out = file.outputStream()

            // Program banks
            val (prgBanksAmount, prgExtra) = calculateProgramBanks(assembler.banks)
            val prgFillData = writeProgramBanksCountInHeader(prgBanksAmount, header)

            // CHR data
            val chrData = mergeCHRData(pcxFiles)
            val (chrBanksAmount, chrExtra) = calculateCHRBanks(chrData.size)
            val chrFillData = writeCHRBanksCountInHeader(chrBanksAmount, header)

            // Write all
            out.write(header.toByteArray())
            writeProgram(out, assembler.banks, prgExtra, prgFillData)
            writeCHR(out, chrData, chrExtra, chrFillData)
            out.close()

            log?.printInfoLn("PRG Size: ${header.prgRomSize} (${header.prgRomSize / 0x4000u} banks)")
            log?.printInfoLn("CHR Size: ${header.chrRomSize} (${header.chrRomSize / 0x2000u} banks)")
        }

        log?.printDoneLn("Assembly successful in $millis ms.")
        return Triple(configuration, assembler, file)
    }

    private fun calculateProgramBanks(banks: Iterable<NESAssemblerMemoryBank>): Pair<Int, Int> {
        val total = banks.filter { it.writeOnCartridge }.sumOf { it.array.size }
        val banksAmount = total shr 14
        val extra = total - (banksAmount shl 14)
        return if (extra > 0) Pair(banksAmount + 1, extra) else Pair(banksAmount, 0)
    }

    private fun writeProgramBanksCountInHeader(banks: Int, header: CartridgeHeader): ULong {
        // Ceil value. This is the first value the normal bank count can't handle
        val ceil = 0b111100000000
        return if (ceil <= banks) {
            // EXPONENTIAL
            val (match) = ExponentialPrgBanksFinder.findBestMatch(banks.toULong() * 0x4000u)
            header.setPrgRomExponential(match.multiplier, match.exponent)
            match.bytes - (banks.toULong() * 0x4000u)
        } else {
            // LINEAR
            header.setPrgRomBanks(banks.toUShort())
            0U
        }
    }

    private fun writeProgram(
        out: OutputStream,
        banks: Iterable<NESAssemblerMemoryBank>,
        extra: Int,
        fillData: ULong
    ) {
        banks.filter { it.writeOnCartridge }.forEach { out.write(it.array.toByteArray()) }
        if (extra > 0) repeat(0x4000 - extra) { out.write(0) }
        for (i in 0UL until fillData) {
            out.write(0)
        }
    }

    private fun mergeCHRData(pcx: Iterable<PictureExchangeImage>): ByteArray {
        val arrays = pcx.map { it.toCHRData() }
        val result = ByteArray(arrays.sumOf { it.size })

        var index = 0
        arrays.forEach {
            System.arraycopy(it, 0, result, index, it.size)
            index += it.size
        }

        return result
    }

    private fun calculateCHRBanks(bytes: Int): Pair<Int, Int> {
        val banksAmount = bytes shr 13
        val extra = bytes - (banksAmount shl 13)
        return if (extra > 0) Pair(banksAmount + 1, extra) else Pair(banksAmount, 0)
    }

    private fun writeCHRBanksCountInHeader(banks: Int, header: CartridgeHeader): ULong {
        // Ceil value. This is the first value the normal bank count can't handle
        val ceil = 0b111100000000
        return if (ceil <= banks) {
            // EXPONENTIAL
            val (match) = ExponentialPrgBanksFinder.findBestMatch(banks.toULong() * 0x2000u)
            header.setChrRomExponential(match.multiplier, match.exponent)
            match.bytes - (banks.toULong() * 0x2000u)
        } else {
            // LINEAR
            header.setChrRomBanks(banks.toUShort())
            0U
        }
    }

    private fun writeCHR(out: OutputStream, data: ByteArray, extra: Int, fillData: ULong) {
        out.write(data)
        if (extra > 0) repeat(0x2000 - extra) { out.write(0) }
        for (i in 0UL until fillData) {
            out.write(0)
        }
    }

    // endregion

    override fun generateSimulation(log: Log?) {
        val (configuration, assembler, file) = assembleToFile(log)
        val test = File(data.filesFolder, "nestest.nes")
        val cartridge = Cartridge(if(test.isFile) test else file)
        val data = NESSimulationData(
            cartridge, Console(), emptyMap(),
            assembler.globalLabels.values.toSet(), configuration
        )

        val simulation = NESSimulation(data)
        Platform.runLater {
            getProjectTab().ifPresent {
                it.projectTabPane.createProjectPane(
                    { t, pt -> NESSimulationPane(t, pt, this, simulation) }, true
                )
            }
        }
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