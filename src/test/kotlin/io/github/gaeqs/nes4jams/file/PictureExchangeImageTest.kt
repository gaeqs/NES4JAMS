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

package io.github.gaeqs.nes4jams.file

import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

private const val TEST_FILE = "https://github.com/captainsouthbird/smb3/blob/master/CHR/chr000.pcx?raw=true"

class PictureExchangeImageTest {

    @Test
    fun checkHeader() {
        val stream = URL(TEST_FILE).openStream()
        val header = PictureExchangeImage.Header(stream)
        stream.close()
        println(header)
        assertTrue(header.version.toInt() == 5, "Version is not 3.0!")
        assertTrue(header.encoding, "Image has no encoding!")
        assertTrue(header.bitsPerPixel.toInt() == 8, "Pixel is not 8bpp!")
    }

    @Test
    fun checkFullImage() {
        val stream = URL(TEST_FILE).openStream()
        val image = PictureExchangeImage(stream)
        val optimized = image.optimizeForNES()
        stream.close()

        for (x in 0 until image.header.width) {
            for (y in 0 until image.header.height) {
                assertEquals(image[x, y], optimized[x, y])
            }
        }


        repeat(30) {
            optimized[it + 5, it] = 0
            optimized[it + 5, 1 + it] = 3

            assertEquals(0, optimized[it + 5, it])
            assertEquals(3, optimized[it + 5, 1 + it])
        }


        val bufferedImage = optimized.toBufferedImage(
            PictureExchangeImage.ColorPalette(
                intArrayOf(
                    0,
                    0xFFFF0000.toInt(),
                    0xFF00FF00.toInt(),
                    0x000000FF
                )
            )
        )
        ImageIO.write(bufferedImage, "PNG", File("testImage.png"))

    }


}