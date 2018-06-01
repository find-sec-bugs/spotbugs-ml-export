package net.gosecure.spotbugs

import net.gosecure.spotbugs.sourcemapper.FileSourceCodeMapper
import net.gosecure.spotbugs.sourcemapper.JavaOnlySourceCodeMapper
import org.apache.http.impl.client.HttpClientBuilder
import picocli.CommandLine
import picocli.CommandLine.Option

import java.io.File
import java.io.FileInputStream

class ExportCli : Runnable {

    @Option(names = arrayOf("-f", "--findbugsfile"), description = arrayOf("FindBugs report"), required = true)
    private val findBugsReport: File? = null
    @Option(names = arrayOf("-g", "-graphfile"), description = arrayOf("Neo4j Graph database"), required = true)
    private val graphDatabase: File? = null
    @Option(names = arrayOf("-c", "-classmapfile"), description = arrayOf("Class mapping file"), required = false)
    private val classMappingFile: File? = null

    @Option(names = arrayOf("-h", "--help"), usageHelp = true, description = arrayOf("Displays this help message and quits."))
    private var helpRequested = false

    override fun run() {

        if (findBugsReport != null && graphDatabase != null) { // && classMappingFile != null
            println("FindBugs report : ${findBugsReport.canonicalPath}")
            println("Graph database  : ${graphDatabase.canonicalPath}")
            //println("Class mapping   : ${classMappingFile.canonicalPath}")

            //////////////////////////////////////////////////


            val logWrapper = LogWrapper()

            val logic = ExportLogic(logWrapper) //Injecting dependencies

            val spotBugsIssues =
                if(classMappingFile != null ) {
                    logic.getSpotBugsIssues(findBugsReport, FileSourceCodeMapper(FileInputStream(classMappingFile),logWrapper))
                }
                else {
                    logic.getSpotBugsIssues(findBugsReport, JavaOnlySourceCodeMapper())
                }

            //Add graph metadata

            logic.graph(spotBugsIssues,graphDatabase)


            logic.updateArtifactIdForAllIssues(spotBugsIssues,"", guessArtifactId(findBugsReport.nameWithoutExtension))

            //Exported to CSV
            val reportName =findBugsReport.nameWithoutExtension + ".csv"
            val csvFile = File(".", reportName)
            csvFile.createNewFile()
            logic.exportCsv(spotBugsIssues, csvFile)

        }
    }

    /**
     * CSV of vulnerabilities are likely to be merge into bigger aggregate dataset.
     * For this reason, it
     */
    fun guessArtifactId(filename:String):String {
        val regex = Regex("([^0-9]+)-([0-9\\.]{3,9})")
        val res = regex.find(filename)?.groupValues?.get(1) ?: ""
        return res
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine.run<Runnable>(ExportCli(), System.out, *args)
        }
    }
}
