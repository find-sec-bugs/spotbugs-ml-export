package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.FindBugsXml
import net.gosecure.spotbugs.datasource.RemoteSonarSource
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name="export-csv")
class ExportMojo : AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private val project: MavenProject? = null

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    override fun execute() {
        //log.info("Hello World from Kotlin Maven plugin !")

        //var isRootPom = project!!.isExecutionRoot()

        val sonarIssues = RemoteSonarSource(log,"http://localhost:9000").getSonarIssues(project!!)
        log.info("Found ${sonarIssues.size} Sonar issues")

        val sonarIssuesLookupTable = HashMap<String,SpotBugsIssue>()
        for(i in sonarIssues) {
            sonarIssuesLookupTable.put(i.getKey(), i)
        }

        if(sonarIssuesLookupTable.size > 0) {

            var spotBugsIssues = FindBugsXml(log).getSpotBugsIssues(project!!)

            log.info("Found ${spotBugsIssues.size} SpotBugs issues")

            val exportedIssues = ArrayList<SpotBugsIssue>()

            for(sbIssue in spotBugsIssues) {

                var existingIssue = sonarIssuesLookupTable.get(sbIssue.getKey())
                if(existingIssue != null) {
                    existingIssue.cwe = sbIssue.cwe
                    existingIssue.methodSink = sbIssue.methodSink
                    existingIssue.unknownSource = sbIssue.unknownSource
                    exportedIssues.add(existingIssue)
                }
                else {
                    log.error("Unable to find the corresponding issue")
                }
            }

            if(exportedIssues.size > 0) {
                val pourcentCoverage = "%.2f".format((exportedIssues.size.toDouble() / spotBugsIssues.size.toDouble()) * 100.toDouble())
                val msg = "${exportedIssues.size} mapped issues from ${spotBugsIssues.size} total SB issues (${pourcentCoverage} %)"
                if(pourcentCoverage=="100") {
                    log.info(msg)
                }
                else {
                    log.warn(msg)
                }

                val buildDir = project!!.build.directory
                val sonarDir = File(buildDir, "sonar")
                val aggregateResults = File(sonarDir, "aggregate-results.csv")

                aggregateResults.createNewFile()

                val writer = aggregateResults.printWriter()
                writer.println("Source File,Line Number,Group Id,Artifact Id,Author,Bug Type,CWE,Method Sink,Unknown Source,Status,Key")
                for(finalIssue in exportedIssues) {
//                    var finalIssue = entry.value
                    writer.println("${finalIssue.sourceFile},${finalIssue.startLine}," +
                            "${finalIssue.groupId},${finalIssue.artifactId}," +
                            "${finalIssue.author},${finalIssue.bugType},"+
                            "${finalIssue.cwe}," +
                            "${finalIssue.methodSink},${finalIssue.unknownSource}," +
                            "${finalIssue.status},${finalIssue.issueKey}")
                }

                writer.flush()
                writer.close()
            }

        }
    }



}