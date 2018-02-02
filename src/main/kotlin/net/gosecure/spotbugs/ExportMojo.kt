package net.gosecure.spotbugs

import org.apache.http.impl.client.HttpClientBuilder
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name="export-csv")
class ExportMojo : AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private lateinit var project: MavenProject

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    override fun execute() {

        val logWrapper = LogWrapper()
        LogWrapper.log = log //Enable Maven logger

        val client = HttpClientBuilder.create().build()
        val logic = ExportLogic(logWrapper,client) //Injecting dependencies

        val groupId    = project.groupId
        val artifactId = project.artifactId


        //Gather sonar issues
        val sonarIssuesLookupTable = logic.getSonarIssues(groupId, artifactId)


        //SpotBugs issues
        //Find the findbugsXml.xml report
        val buildDir = project!!.build.directory as String
        val sonarDir = File(buildDir, "sonar")

        if (!sonarDir.exists()) {
            log.warn("No sonar directory found in the project ${project!!.basedir}. Sonar must be runned prior to the export.")
            return
        }

        val findbugsResults = getFindBugsResultFileOnMaven(buildDir)
        val classMappingFile = File(sonarDir, "class-mapping.csv")


        var spotBugsIssues = logic.getSpotBugsIssues(findbugsResults,classMappingFile)


        //Sonar + SpotBugs
        var exportedIssues = logic.enrichSonarExportIssue(sonarIssuesLookupTable, spotBugsIssues)


        //Add graph metadata
        val fileGraph = getGraphFile(buildDir)

        if(fileGraph == null) {
            log.error("Graph database not found. (codegraph.db)")
        }
        else {
            log.info("Using graph database located at ${fileGraph.path}")
            logic.graph(exportedIssues,fileGraph)
        }

        //Report coverage (sonar and SpotBugs combined)
        logic.reportCoverage(exportedIssues, spotBugsIssues)


        //Exported to CSV
        val aggregateResults = File(sonarDir, "aggregate-results.csv")
        aggregateResults.createNewFile()
        val sonarMlDir = File(buildDir, "spotbugs-ml")
        logic.exportCsv(exportedIssues, aggregateResults)


    }




    /**
     * Look at parent directory to find the graph present at the root directory.
     * TODO: Make a more elegant solution
     * @return Database directory or null if not found
     */
    private fun getGraphFile(baseDir:String) : File? {
        val testDir = File(baseDir)
        val testGraphFile = File(baseDir,"codegraph.db")
        if(testGraphFile.isDirectory) {
            return testGraphFile
        }
        else {
            val parentDir = testDir.parent ?: return null
            return getGraphFile(parentDir)
        }
    }


    private fun getFindBugsResultFileOnMaven(buildDir:String): File {
        val mvnFbPluginFile    = File(buildDir, "findbugsXml.xml")
        val mvnSonarPluginFile = File(buildDir, "sonar/findbugs-result.xml")
        return if (mvnFbPluginFile.exists()) mvnFbPluginFile  else mvnSonarPluginFile
    }

    fun emptyIfNull(value:String?):String = value ?: ""



}