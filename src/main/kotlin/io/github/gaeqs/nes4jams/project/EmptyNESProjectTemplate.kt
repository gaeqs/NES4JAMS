package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.data.NES4JAMS_PROJECT_TEMPLATE_NES_EMPTY
import io.github.gaeqs.nes4jams.data.PLUGIN_ICON
import javafx.beans.property.BooleanProperty
import javafx.scene.Node
import javafx.scene.image.Image
import net.jamsimulator.jams.configuration.RootConfiguration
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.configuration.ConfigurationRegionDisplay
import net.jamsimulator.jams.gui.util.PathAndNameEditor
import net.jamsimulator.jams.project.ProjectData
import net.jamsimulator.jams.project.ProjectTemplate
import net.jamsimulator.jams.project.ProjectTemplateBuilder
import net.jamsimulator.jams.project.exception.MIPSTemplateBuildException
import net.jamsimulator.jams.utils.FolderUtils
import java.io.File

class EmptyNESProjectTemplate : ProjectTemplate<NESProject>(NESProjectType.INSTANCE) {

    private val editor = PathAndNameEditor()

    init {
        editor.children.add(0, ConfigurationRegionDisplay(EmptyNESProjectTemplateBuilder.LANGUAGE_NODE))
    }

    override fun getBuilderNode(): Node = editor

    override fun validProperty(): BooleanProperty = editor.validProperty()

    override fun build(): NESProject {
        val folder = File(editor.path)
        if (!FolderUtils.checkFolder(folder))
            throw MIPSTemplateBuildException("Couldn't create folder " + editor.path + "!")
        val metadataFolder = File(folder, ProjectData.METADATA_FOLDER_NAME)
        if (!FolderUtils.checkFolder(metadataFolder))
            throw MIPSTemplateBuildException("Couldn't create folder " + metadataFolder.absolutePath + "!")

        val metadataFile = File(metadataFolder, ProjectData.METADATA_DATA_NAME)
        val config = RootConfiguration(metadataFile)
        // DEFAULT DATA HERE
        config.save(true)

        return try {
            NESProject(folder)
        } catch (e: Exception) {
            throw MIPSTemplateBuildException(e)
        }
    }
}

class EmptyNESProjectTemplateBuilder : ProjectTemplateBuilder<NESProject>(NAME, LANGUAGE_NODE, ICON) {

    companion object {
        const val NAME = "nes-empty"
        val LANGUAGE_NODE = NES4JAMS_PROJECT_TEMPLATE_NES_EMPTY
        val ICON: Image? = JamsApplication.getIconManager().getOrLoadSafe(PLUGIN_ICON).orElse(null)
    }

    override fun createBuilder(): ProjectTemplate<NESProject> = EmptyNESProjectTemplate()

}