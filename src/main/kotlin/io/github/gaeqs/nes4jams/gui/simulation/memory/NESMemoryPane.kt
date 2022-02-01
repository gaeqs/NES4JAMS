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

import io.github.gaeqs.nes4jams.gui.simulation.memory.representation.NESNumberRepresentation
import io.github.gaeqs.nes4jams.gui.simulation.memory.view.NESMemoryView
import io.github.gaeqs.nes4jams.gui.simulation.memory.view.NESMemoryViewManager
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.managerOf
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.ActionRegion
import net.jamsimulator.jams.gui.action.RegionTags
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.gui.util.LanguageComboBox
import net.jamsimulator.jams.manager.event.ManagerElementRegisterEvent
import net.jamsimulator.jams.manager.event.ManagerElementUnregisterEvent

class NESMemoryPane(val simulation: NESSimulation) : AnchorPane(), ActionRegion {

    private val views = arrayListOf<NESMemoryView>()
    private val viewSelector = LanguageComboBox<NESMemoryView> { it.languageNode }

    private val representations = arrayListOf<NESNumberRepresentation>()
    private val representationSelector = LanguageComboBox<NESNumberRepresentation> { it.languageNode }

    private val table: NESMemoryTable

    val representation get() = representationSelector.selectionModel.selectedItem!!
    val view get() = viewSelector.selectionModel.selectedItem!!

    private val headerHBox = HBox().apply {
        spacing = 5.0
        children.addAll(viewSelector, representationSelector)
        viewSelector.prefWidthProperty().bind(widthProperty().divide(2))
        representationSelector.prefWidthProperty().bind(widthProperty().divide(2))
        AnchorUtils.setAnchor(this, 0.0, -1.0, 2.0, 2.0)
    }

    private val buttonsHBox = HBox().apply {
        alignment = Pos.CENTER
        isFillHeight = true
        AnchorUtils.setAnchor(this, -1.0, 0.0, 2.0, 2.0)
    }

    init {
        initRepresentationComboBox()
        initButtons()

        table = NESMemoryTable(this)
        AnchorUtils.setAnchor(table, 60.0, 31.0, 0.0, 0.0)

        children.addAll(headerHBox, table, buttonsHBox)


        managerOf<NESNumberRepresentation>().registerListeners(this, true)
        managerOf<NESMemoryView>().registerListeners(this, true)
    }

    override fun supportsActionRegion(region: String?) = region == RegionTags.MIPS_SIMULATION


    private fun initRepresentationComboBox() {
        views += managerOf<NESMemoryView>()
        views.sortBy { it.name }
        viewSelector.items += views
        viewSelector.selectionModel.select(NESMemoryView.CPU)
        viewSelector.setOnAction { table.entries.values.forEach { it.refresh() } }

        representations += managerOf<NESNumberRepresentation>()
        representations.sortBy { it.name }
        representationSelector.items += representations
        representationSelector.selectionModel.select(NESNumberRepresentation.HEXADECIMAL)
        representationSelector.setOnAction { table.entries.values.forEach { it.refresh() } }
    }

    private fun initButtons() {
        val previous = Button("\u2190")
        val next = Button("\u2192")
        previous.styleClass += "bold-button"
        next.styleClass += "bold-button"
        previous.prefWidth = 300.0
        next.prefWidth = 300.0

        previous.setOnAction { table.previousPage() }
        next.setOnAction { table.nextPage() }

        buttonsHBox.children.addAll(previous, next)
    }

    // region events

    @Listener
    private fun onRepresentationRegister(event: ManagerElementRegisterEvent.After<NESNumberRepresentation>) {
        representations += event.element
        refreshRepresentationComboBox()
    }

    @Listener
    private fun onRepresentationUnregister(event: ManagerElementUnregisterEvent.After<NESNumberRepresentation>) {
        representations -= event.element
        refreshRepresentationComboBox()
    }

    private fun refreshRepresentationComboBox() {
        representations.sortBy { it.name }
        representationSelector.items.setAll(representations)
        representationSelector.selectionModel.select(NESNumberRepresentation.HEXADECIMAL)
    }

    @Listener
    private fun onViewRegister(event: ManagerElementRegisterEvent.After<NESMemoryView>) {
        views += event.element
        refreshViewComboBox()
    }

    @Listener
    private fun onViewUnregister(event: ManagerElementUnregisterEvent.After<NESMemoryView>) {
        views -= event.element
        refreshViewComboBox()
    }

    private fun refreshViewComboBox() {
        views.sortBy { it.name }
        viewSelector.items.setAll(views)
        viewSelector.selectionModel.select(NESMemoryView.CPU)
    }

    // endregion
}