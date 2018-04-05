package net.gosecure.spotbugs

import org.apache.http.impl.client.HttpClientBuilder
import picocli.CommandLine
import picocli.CommandLine.Option

import java.io.File

class ExportCli : Runnable {

    @Option(names = arrayOf("-f", "--findbugsfile"), description = arrayOf("FindBugs report"), required = true)
    private val findBugsReport: File? = null
    @Option(names = arrayOf("-g", "-graphfile"), description = arrayOf("Neo4j Graph database"), required = true)
    private val graphDatabase: File? = null
    @Option(names = arrayOf("-c", "-classmapfile"), description = arrayOf("Class mapping file"), required = true)
    private val classMappingFile: File? = null

    @Option(names = arrayOf("-h", "--help"), usageHelp = true, description = arrayOf("Displays this help message and quits."))
    private var helpRequested = false

    override fun run() {

        if (findBugsReport != null && graphDatabase != null && classMappingFile != null) {
            println("FindBugs report : ${findBugsReport.canonicalPath}")
            println("Graph database  : ${graphDatabase.canonicalPath}")
            println("Class mapping   : ${classMappingFile.canonicalPath}")

            //////////////////////////////////////////////////


            val logWrapper = LogWrapper()

            val logic = ExportLogic(logWrapper) //Injecting dependencies

            var spotBugsIssues = logic.getSpotBugsIssues(findBugsReport,classMappingFile)

            //Add graph metadata

            logic.graph(spotBugsIssues,graphDatabase)

            //Exported to CSV
            val csvFile = File(".", "aggregate-results.csv")
            csvFile.createNewFile()
            logic.exportCsv(spotBugsIssues, csvFile)

        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine.run<Runnable>(ExportCli(), System.out, *args)
        }
    }
}
