package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.FindBugsXml
import net.gosecure.spotbugs.datasource.Neo4jGraph
import net.gosecure.spotbugs.datasource.RemoteSonarSource
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import java.io.File
import java.io.IOException

@Mojo(name="export-csv")
class ExportMojo : AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private val project: MavenProject? = null

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    override fun execute() {

        //var isRootPom = project!!.isExecutionRoot()


        var exportedIssues = ArrayList<SpotBugsIssue>()

        //Sonar Export

        var sonarIssues:List<SpotBugsIssue> = ArrayList()
        try {
            sonarIssues = RemoteSonarSource(log, "http://localhost:9000").getSonarIssues(project!!)
            log.info("Found ${sonarIssues.size} Sonar issues")
        }
        catch (ioe: IOException){
            log.warn("Skipping sonar data import")
        }

        val sonarIssuesLookupTable = HashMap<String,SpotBugsIssue>()
        for(i in sonarIssues) {
            sonarIssuesLookupTable.put(i.getKey(), i)
        }

        //SpotBugs

        var spotBugsIssues = FindBugsXml(log).getSpotBugsIssues(project!!)

        if(sonarIssuesLookupTable.size > 0) {


            log.info("Found ${spotBugsIssues.size} SpotBugs issues")


            //Integrating SonarQube metadata
            for (sbIssue in spotBugsIssues) {

                var existingIssue = sonarIssuesLookupTable.get(sbIssue.getKey())
                if (existingIssue != null) {
                    existingIssue.cwe = sbIssue.cwe
                    existingIssue.methodSink = sbIssue.methodSink
                    existingIssue.unknownSource = sbIssue.unknownSource
                    existingIssue.sourceMethod = sbIssue.sourceMethod

                    exportedIssues.add(existingIssue)
                } else {
                    log.error("Unable to find the corresponding issue")
                }
            }
        } else {
            exportedIssues = spotBugsIssues as ArrayList<SpotBugsIssue>
            log.warn("Using only SpotBugs as data source (${spotBugsIssues.size}) issues added")
        }


        val buildDirectory = project!!.build.directory

        //Integrating Neo4j metadata
        val fileGraph = getGraphFile(buildDirectory)

        if(fileGraph == null) {
            log.error("Graph database not found. (codegraph.db)")
        }
        else { log.info("Using graph database located at ${fileGraph.path}")
            val db = GraphDatabaseFactory().newEmbeddedDatabase(fileGraph)
            try {
                val graphDb = Neo4jGraph(db)
                for (issue in exportedIssues) {
                    if (issue.methodSink != "") {
                        var nodesTainted = graphDb.searchTaintSource(issue.methodSink, issue.sourceMethod,"TAINTED")
                        issue.hasTaintedSource = nodesTainted.isNotEmpty()

                        var nodesSafe = graphDb.searchTaintSource(issue.methodSink, issue.sourceMethod,"SAFE")
                        issue.hasSafeSource = nodesSafe.isNotEmpty()

                        var nodesUnknown = graphDb.searchTaintSource(issue.methodSink, issue.sourceMethod,"UNKNOWN")
                        issue.hasUnknownSource = nodesUnknown.isNotEmpty()
                    }
                }
            }
            finally {
                db.shutdown();
            }
        }

        //Exported to CSV
        if(exportedIssues.size > 0) {
            val pourcentCoverage = "%.2f".format((exportedIssues.size.toDouble() / spotBugsIssues.size.toDouble()) * 100.toDouble())
            val msg = "${exportedIssues.size} mapped issues from ${spotBugsIssues.size} total SB issues (${pourcentCoverage} %)"
            if((spotBugsIssues.size - exportedIssues.size) == 0) {
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
            writer.println("Source File,Line Number,Group Id,Artifact Id,Author,Bug Type,CWE,Method Sink,Unknown Source,Status,Has Tainted Source,Has Safe Source,Key")
            for(finalIssue in exportedIssues) {
//                    var finalIssue = entry.value
                writer.println("${finalIssue.sourceFile},${finalIssue.startLine}," +
                        "${finalIssue.groupId},${finalIssue.artifactId}," +
                        "${finalIssue.author},${finalIssue.bugType},"+
                        "${finalIssue.cwe}," +
                        "${finalIssue.methodSink},${finalIssue.unknownSource}," +
                        "${finalIssue.status}," +
                        "${finalIssue.hasTaintedSource},${finalIssue.hasSafeSource}," +
                        "${finalIssue.issueKey}")
            }

            writer.flush()
            writer.close()
        }


    }

    /**
     * Look at parent directory to find the graph present at the root directory.
     * TODO: Make a more elegant solution
     * @return Database directory or null if not found
     */
    fun getGraphFile(baseDir:String) : File? {
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


}