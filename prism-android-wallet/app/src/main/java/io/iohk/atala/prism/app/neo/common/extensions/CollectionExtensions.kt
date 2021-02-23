package io.iohk.atala.prism.app.neo.common.extensions

/**
 * Returns the list of indexes in a random order
 * @return a list indexes in a random order
 */
fun <T> Collection<T>.randomIndexes(): List<Int> {
    return indices.toList().shuffled()
}
