package io.github.gaeqs.nes4jams.utils

class RangeCollection : Collection<IntRange> {

    private val collection = sortedSetOf<IntRange>({ o1, o2 -> o1.first - o2.first })

    override val size: Int get() = collection.size

    fun addAll(map: Iterable<IntRange>) {
        for (element in map) add(element)
    }

    fun add(element: IntRange) {
        if (element.isEmpty()) return

        val containsFirst = collection.find { element.first in it }
        if (containsFirst != null) {
            concatenateAtStart(element, containsFirst)
            return
        }

        val containsLast = collection.find { element.last in it }
        if (containsLast != null) {
            concatenateAtEnd(element, containsLast)
            return
        }

        collection.filter { it.first in element && it.last in element }.forEach { collection -= it }
        collection += element
    }

    fun invert(fromInclusive: Int, toInclusive: Int): RangeCollection {
        if (toInclusive < fromInclusive) {
            throw IllegalArgumentException("from < to.")
        }
        val inverse = RangeCollection()

        if (toInclusive == fromInclusive) {
            if (toInclusive !in this) inverse.add(IntRange(toInclusive, toInclusive))
            return inverse
        }

        val first = find { fromInclusive <= it.last }
        val last = findLast { toInclusive >= it.first }

        if (first == null || last == null) {
            inverse.add(IntRange(fromInclusive, toInclusive))
            return inverse
        }

        // Add first
        inverse.add(IntRange(fromInclusive, first.first - 1))

        // And intermediate
        val set = collection.subSet(first, true, last, true).zipWithNext()
        set.forEach { inverse.add(IntRange(it.first.last + 1, it.second.first - 1)) }

        // Add last
        inverse.add(IntRange(last.last + 1, toInclusive))

        return inverse
    }

    operator fun contains(element: Int): Boolean {
        return collection.any { element in it }
    }

    override fun contains(element: IntRange): Boolean {
        return collection.add(element)
    }

    override fun containsAll(elements: Collection<IntRange>): Boolean {
        return collection.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return collection.isEmpty()
    }

    override fun iterator(): Iterator<IntRange> {
        return collection.iterator()
    }

    override fun toString(): String {
        return collection.toString()
    }

    private fun concatenateAtStart(element: IntRange, concatenateTo: IntRange) {
        if (element.last in concatenateTo) return
        collection -= concatenateTo

        var result = IntRange(concatenateTo.first, element.last)

        val next = collection.tailSet(concatenateTo, false).firstOrNull()
        if (next != null && element.last in next) {
            collection -= next
            result = IntRange(concatenateTo.first, next.last)
        }

        collection += result

        return
    }

    private fun concatenateAtEnd(element: IntRange, concatenateTo: IntRange) {
        if (element.first in concatenateTo) return
        collection -= concatenateTo

        var result = IntRange(element.first, concatenateTo.last)

        val previous = collection.headSet(concatenateTo, false).lastOrNull()
        if (previous != null && element.first in previous) {
            collection -= previous
            result = IntRange(previous.first, concatenateTo.last)
        }

        collection += result

        return
    }
}