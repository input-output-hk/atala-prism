package io.iohk.atala

import io.iohk.atala.prism.app.neo.common.extensions.randomIndexes
import org.junit.Assert.*
import org.junit.Test

class CollectionExtensionsTest {

    @Test
    fun randomIndexesTest() {
        val list = listOf("A", "B", "C", "D", "E", "F", "G", "H")

        val indexes = list.randomIndexes()

        // check size of indexes
        assertEquals(indexes.size, 8)
        // check size of none duplicated indexes
        assertEquals(HashSet<Int>(indexes).size, 8)
        indexes.forEach {
            assertTrue(it in 0..7)
        }
    }
}