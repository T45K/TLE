package io.github.t45k.nil

import io.github.t45k.nil.entity.CodeBlock
import io.github.t45k.nil.output.CSV
import io.github.t45k.nil.tokenizer.SymbolSeparator
import io.github.t45k.nil.tokenizer.Tokenizer
import io.github.t45k.nil.util.ProgressMonitor
import io.github.t45k.nil.util.toTime
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.toObservable
import java.io.File

// 一旦リストに保持する
// スケーラビリティを考えると将来的にDBを使うかも
// IDはリストとかDBのインデックスで大丈夫そう
class NILMain(private val config: NILConfig) {

    private val tokenizer: Tokenizer = SymbolSeparator()

    fun run() {
        val startTime = System.currentTimeMillis()
        val codeBlocks: List<CodeBlock> = collectSourceFiles(config.src)
            .flatMap(this::collectBlocks)
            .toList()
            .blockingGet()

        println("${codeBlocks.size} code blocks have been extracted in ${((System.currentTimeMillis() - startTime) / 1000).toTime()}.\n")

        val location = Location(config.filteringThreshold, codeBlocks)
        val verification = Verification(codeBlocks)
        val progressMonitor = ProgressMonitor(codeBlocks.size)
        val clonePairs: List<Pair<Int, Int>> = codeBlocks
            .flatMapIndexed { index, codeBlock ->
                val clonePairs: List<Pair<Int, Int>> = location.locate(codeBlock.nGrams)
                    .filter { verification.verify(index, it) }
                    .map { index to it }

                location.put(codeBlock.nGrams, index)
                progressMonitor.update(index + 1)

                clonePairs
            }
            .toList()

        println("${clonePairs.size} clone pairs are detected.")

        val endTime = System.currentTimeMillis()
        println("time: ${((endTime - startTime) / 1000).toTime()}")

        CSV().output(config.outputFileName, clonePairs, codeBlocks)
    }

    private fun collectSourceFiles(dir: File): Observable<File> =
        dir.walk()
            .filter { it.isFile && it.toString().endsWith(".java") }
            .toObservable()

    private fun collectBlocks(sourceFile: File): Observable<CodeBlock> =
        Observable.just(sourceFile)
            .flatMap { AST(tokenizer::tokenize, config).extractBlocks(it) }
}

fun main(args: Array<String>) {
    val config: NILConfig = parseArgs(args)
    NILMain(config).run()
}