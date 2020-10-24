package io.github.t45k.nil

import io.github.t45k.nil.entity.CodeBlock
import io.github.t45k.nil.entity.TokenSequence
import kotlin.math.min

class Location(private val filteringThreshold: Int, private val codeBlocks: List<CodeBlock>) {

    private val hashTable: MutableMap<Int, MutableList<Int>> = HashMap(500_000)

    fun locate(tokenSequence: TokenSequence): List<Int> =
        tokenSequence
            .flatMap { hashTable[it] ?: emptyList() }
            .groupingBy { it }
            .eachCount()
            .filter {
                it.value * 100 / min(
                    tokenSequence.size,
                    codeBlocks[it.key].tokenSequence.size
                ) >= filteringThreshold
            }
            .keys
            .toList()

    fun put(tokenSequence: TokenSequence, id: Int) =
        tokenSequence.forEach { hashTable.getOrPut(it) { mutableListOf() }.add(id) }

    fun clear() = hashTable.clear()
}
