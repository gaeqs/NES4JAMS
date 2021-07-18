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

package io.github.gaeqs.nes4jams.gui.configuration

import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationAddEvent
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationRemoveEvent
import io.github.gaeqs.nes4jams.util.extension.fit
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.explorer.Explorer
import net.jamsimulator.jams.gui.explorer.ExplorerBasicElement
import net.jamsimulator.jams.gui.explorer.ExplorerSection
import net.jamsimulator.jams.gui.image.NearestImageView
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.gui.util.PixelScrollPane
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.language.wrapper.LanguageTooltip

class NESConfigurationList(val window: NESConfigurationWindow) : AnchorPane() {

    val contents: NESConfigurationListContents
    val controls : NESConfigurationListControls

    init {
        SplitPane.setResizableWithParent(this, true)

        controls = NESConfigurationListControls(this)

        val scroll = PixelScrollPane().fit()
        contents = NESConfigurationListContents(window, scroll)
        scroll.content = contents

        AnchorUtils.setAnchor(controls, 0.0, -1.0, 0.0, 0.0)
        AnchorUtils.setAnchor(scroll, 30.0, 0.0, 0.0, 0.0)
        children.addAll(controls, scroll)
    }

}

class NESConfigurationListControls(val list: NESConfigurationList) : HBox() {

    companion object {
        const val ICON_SIZE = 16.0
    }

    init {
        children.addAll(createAddButton(), createRemoveButton(), createCopyButton())
    }

    private fun createAddButton(): Button {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.CONTROL_ADD).orElse(null)
        val button = Button(null, NearestImageView(icon, ICON_SIZE, ICON_SIZE))
        button.tooltip = LanguageTooltip(Messages.GENERAL_ADD)
        button.styleClass += "bold-button"
        button.setOnAction {
            val baseName = "New Configuration"
            var name = baseName
            var amount = 1

            while (list.window.data.configurations.any { it.name == name }) {
                name = "$baseName (${amount++})"
            }
            list.window.data.addConfiguration(NESSimulationConfiguration(name))
        }
        return button
    }

    private fun createRemoveButton(): Button {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.CONTROL_REMOVE).orElse(null)
        val button = Button(null, NearestImageView(icon, ICON_SIZE, ICON_SIZE))
        button.tooltip = LanguageTooltip(Messages.GENERAL_REMOVE)
        button.styleClass += "bold-button"
        button.setOnAction {
            val selected = list.contents.selectedElements
            if (selected.isEmpty()) return@setOnAction
            selected.forEach { list.window.data.removeConfiguration(it.name) }
            list.contents.mainSection.getElementByIndex(0)
                .ifPresentOrElse({ list.contents.selectElementAlone(it) }, { list.window.display(null) })
        }
        return button
    }

    private fun createCopyButton(): Button {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.CONTROL_COPY).orElse(null)
        val button = Button(null, NearestImageView(icon, ICON_SIZE, ICON_SIZE))
        button.tooltip = LanguageTooltip(Messages.GENERAL_REMOVE)
        button.styleClass += "bold-button"
        button.setOnAction {
            list.contents.selectedElements.forEach {
                if(it is NESConfigurationListContents.Representation) {
                    val baseName = it.configuration.name + " - Copy"
                    var name = baseName
                    var amount = 1

                    while (list.window.data.configurations.any { t -> t.name == name }) {
                        name = "$baseName (${amount++})"
                    }
                    list.window.data.addConfiguration(NESSimulationConfiguration(name, it.configuration))
                }
            }
        }
        return button
    }

}

class NESConfigurationListContents(val window: NESConfigurationWindow, scrollPane: ScrollPane) :
    Explorer(scrollPane, false, false) {

    init {
        generateMainSection()
        hideMainSectionRepresentation()
        window.data.registerListeners(this, true)
    }

    fun refreshName(configuration: NESSimulationConfiguration) {
        mainSection.removeElementIf { it is Representation && it.configuration == configuration }
        val representation = Representation(configuration, mainSection)
        mainSection.addElement(representation)
        selectElementAlone(representation)
    }

    fun selectFirst() {
        if (!mainSection.isEmpty) {
            mainSection.getElementByIndex(0).ifPresent { selectElementAlone(it) }
        }
    }

    override fun generateMainSection() {
        mainSection = ExplorerSection(this, null, "", 0)
        { o1, o2 -> o1.name.compareTo(o2.name) }

        window.data.configurations.forEach { mainSection.addElement(Representation(it, mainSection)) }
        children += mainSection
    }

    @Listener
    private fun onConfigurationAdd(event: NESSimulationConfigurationAddEvent.After) {
        val wasEmpty = mainSection.isEmpty
        val representation = Representation(event.configuration, mainSection)
        mainSection.addElement(representation)
        if (wasEmpty) selectElementAlone(representation)
    }

    @Listener
    private fun onConfigurationRemove(event: NESSimulationConfigurationRemoveEvent.After) {
        mainSection.removeElementIf { it is Representation && it.configuration == event.configuration }
    }

    inner class Representation(val configuration: NESSimulationConfiguration, parent: ExplorerSection) :
        ExplorerBasicElement(parent, configuration.name, 1) {

        override fun select() {
            super.select()
            window.display(configuration)
        }

    }
}