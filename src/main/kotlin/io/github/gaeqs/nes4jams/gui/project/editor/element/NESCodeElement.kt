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

package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.extension.DEFAULT_LANGUAGE
import io.github.gaeqs.nes4jams.utils.extension.get
import net.jamsimulator.jams.language.Language

abstract class NESCodeElement(val line: NESLine, val text: String, var startIndex: Int, var endIndex: Int) {

    abstract val translatedNameNode: String
    abstract val simpleText: String
    abstract val styles: List<String>

    val translatedName get() = getTranslatedName(DEFAULT_LANGUAGE)

    fun getTranslatedName(language: Language): String {
        return language[translatedNameNode]
    }

    open fun move(offset: Int) {
        startIndex += offset
        endIndex += offset
    }

    fun getGeneralStyles(vararg baseStyles: String): MutableList<String> {
        val list = baseStyles.toMutableList()
        //if (hasInspections()) {
        //    list.add(if (hasErrors()) "mips-error" else "mips-warning")
        //}
        return list
    }

}