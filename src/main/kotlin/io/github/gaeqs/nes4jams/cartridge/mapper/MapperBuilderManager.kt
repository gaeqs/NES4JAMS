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

package io.github.gaeqs.nes4jams.cartridge.mapper

import io.github.gaeqs.nes4jams.NES4JAMS
import io.github.gaeqs.nes4jams.cartridge.mapper.defaults.*
import net.jamsimulator.jams.manager.Manager
import net.jamsimulator.jams.manager.ResourceProvider

class MapperBuilderManager private constructor(provider: ResourceProvider, loadOnFxThread: Boolean) :
    Manager<MapperBuilder<*>>(provider, NAME, MapperBuilder::class.java, loadOnFxThread) {

    companion object {
        val NAME = "mapper_builder"
        val INSTANCE = MapperBuilderManager(NES4JAMS.INSTANCE, true)
    }


    override fun loadDefaultElements() {
        add(Mapper000.Builder.INSTANCE)
        add(Mapper001.Builder.INSTANCE)
        add(Mapper002.Builder.INSTANCE)
        add(Mapper003.Builder.INSTANCE)
        add(Mapper004.Builder.INSTANCE)

        add(Mapper011.Builder.INSTANCE)

        add(Mapper066.Builder.INSTANCE)
    }
}