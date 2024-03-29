import groovy.json.JsonSlurper
import net.steppschuh.markdowngenerator.table.Table

import java.nio.file.Files
import java.nio.file.Path

@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
        @Grab('org.apache.groovy:groovy-json:4.0.13'),
        @Grab('com.github.Steppschuh:Java-Markdown-Generator:1.3.2')
])

final dummyClassResults = [:]
final noopResults = [:]

final resultsPath = Path.of('jmh_results')
Files.list(resultsPath).map { it.resolve('result.json') }
        .map { [new JsonSlurper().parse(it.toFile()), it.parent.fileName.toString().substring('jmh_'.length())] }
        .forEach {
            dummyClassResults[it[1]] = "${it[0][0].primaryMetric.score.round(2)} ± ${it[0][0].primaryMetric.scoreError.round(2)} ${it[0][0].primaryMetric.scoreUnit}"
            noopResults[it[1]] = "${it[0][1].primaryMetric.score.round(2)} ± ${it[0][1].primaryMetric.scoreError.round(2)} ${it[0][1].primaryMetric.scoreUnit}"
        }

Table.Builder dummyClassTable = new Table.Builder()
        .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
        .addRow('JDK name & Version', 'Benchmark results')
dummyClassResults.sort { a, b -> a.value <=> b.value }.forEach { type, results -> dummyClassTable.addRow(type, results) }

Table.Builder noopTable = new Table.Builder()
        .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
        .addRow('JDK name & Version', 'Benchmark results')
noopResults.sort { a, b -> a.value <=> b.value }.forEach { type, results -> noopTable.addRow(type, results) }

new File('jmh_results.md').text = """
# `cpw.mods.modlauncher.benchmarks.TransformBenchmark.transformDummyClass` results
${dummyClassTable.build()}

# `cpw.mods.modlauncher.benchmarks.TransformBenchmark.transformNoop` results
${noopTable.build()}
"""