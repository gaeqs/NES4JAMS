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

package io.github.gaeqs.nes4jams.gui.simulation.memory

import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.configuration.event.ConfigurationNodeChangeEvent
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.language.wrapper.LanguageTableColumn
import net.jamsimulator.jams.mips.simulation.event.SimulationResetEvent
import net.jamsimulator.jams.mips.simulation.event.SimulationStopEvent

class NESMemoryTable(
    val pane: NESMemoryPane
) : TableView<NESMemoryTableEntry>() {

    companion object {
        val MEMORY_ROWS_CONFIGURATION_NODE = "simulation.memory_rows"
    }

    internal val entries = hashMapOf<UShort, NESMemoryTableEntry>()
    private var rows: Int

    var offset: UShort = 0u
        set(value) {
            field = value
            populate()
        }

    init {
        styleClass += "table-view-horizontal-fit"
        isEditable = true
        columnResizePolicy = CONSTRAINED_RESIZE_POLICY


        val pAddress = LanguageTableColumn<NESMemoryTableEntry, String>(Messages.MEMORY_ADDRESS).configure(-1)
        val p0 = TableColumn<NESMemoryTableEntry, String>("+0").configure(0)
        val p1 = TableColumn<NESMemoryTableEntry, String>("+1").configure(1)
        val p2 = TableColumn<NESMemoryTableEntry, String>("+2").configure(2)
        val p3 = TableColumn<NESMemoryTableEntry, String>("+3").configure(3)
        columns.addAll(pAddress, p0, p1, p2, p3)
        getVisibleLeafColumn(0).minWidth = 80.0

        rows = Jams.getMainConfiguration().get<Int>(MEMORY_ROWS_CONFIGURATION_NODE).orElse(47)

        populate()

        Jams.getMainConfiguration().registerListeners(this, true)
        pane.simulation.registerListeners(this, true)
    }

    fun populate() {
        items.clear()
        var current = offset
        repeat(rows) {
            val entry = NESMemoryTableEntry(this, current)
            entries[current] = entry
            items += entry
            current = (current + 4u).toUShort()
        }
    }

    fun nextPage() {
        offset = (offset + (rows shl 2).toUShort()).toUShort()
    }

    fun previousPage() {
        offset = (offset - (rows shl 2).toUShort()).toUShort()
    }

    private fun TableColumn<NESMemoryTableEntry, String>.configure(offset: Int): TableColumn<NESMemoryTableEntry, String> {
        isSortable = false

        if (offset == -1) {
            id = "address"
            setCellValueFactory { it.value.addressProperty }
        } else {
            id = offset.toString()
            setCellValueFactory { it.value.propertyByIndex(offset) }
            setCellFactory { NESMemoryTableCell(offset) }
        }
        return this
    }

    @Listener
    private fun onSimulationStop(event: SimulationStopEvent) {
        entries.values.forEach { it.refresh() }
        refresh()
    }

    @Listener
    private fun onSimulationReset(event: SimulationResetEvent) {
        entries.values.forEach { it.refresh() }
    }

    @Listener
    private fun onConfigurationNodeChange(event: ConfigurationNodeChangeEvent.After) {
        if (event.node == MEMORY_ROWS_CONFIGURATION_NODE) {
            rows = event.getNewValueAs<Int>().orElse(rows)
            populate()
        }
    }


}