/*
 *  MIT License
 *
 *  Copyright (c) 2022 Gael Rial Costas
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

import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.language.Language
import net.jamsimulator.jams.manager.Manager
import net.jamsimulator.jams.manager.ManagerResource
import net.jamsimulator.jams.manager.ResourceProvider
import net.jamsimulator.jams.manager.event.ManagerElementRegisterEvent
import net.jamsimulator.jams.project.Project
import net.jamsimulator.jams.task.LanguageTask

data class MyElement(
    private val name: String,
    private val provider: ResourceProvider
) : ManagerResource {
    override fun getName() = name
    override fun getResourceProvider() = provider
}

class MyManager(provider: ResourceProvider) : Manager<MyElement>(
    provider,
    "my-manager",
    MyElement::class.java,
    false
) {
    override fun loadDefaultElements() {}
}

class ObjectWithListeners {

    fun startTask(project: Project) {
        project.taskExecutor.execute(LanguageTask.of("MY_TITLE_LANGUAGE_NODE") {
            Thread.sleep(1000)

            // Do things here!

            Thread.sleep(1000)
        })
    }

    @Listener
    fun onLanguageRegister(event: ManagerElementRegisterEvent.After<Language>) {
        println("New language available! ${event.element.name}")
    }

}