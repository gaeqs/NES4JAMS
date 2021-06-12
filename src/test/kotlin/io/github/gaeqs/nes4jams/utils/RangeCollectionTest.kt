package io.github.gaeqs.nes4jams.utils

import org.junit.jupiter.api.Test

class RangeCollectionTest {

    @Test
    fun testRange() {
        val collection = RangeCollection()
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(10, 20))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(10, 5))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(10, 15))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(5, 15))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(25, 30))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(23, 31))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(0, 50))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(75, 80))
        println(collection)
        println("- " + collection.invert(0, 150))

        collection.add(IntRange(50, 75))
        println(collection)
        println("- " + collection.invert(0, 150))
    }


}

