package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.FindBugsReportSource
import net.gosecure.spotbugs.datasource.Neo4jGraph
import net.gosecure.spotbugs.datasource.RemoteSonarSource
import net.gosecure.spotbugs.model.SonarConnectionInfo
import net.gosecure.spotbugs.model.SpotBugsIssue
import org.apache.http.client.HttpClient
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * This class should be agnostic
 */
class ExportLogic(var log:LogWrapper,val client: HttpClient) {

    fun getSonarIssues(groupdId: String, artifactId: String): HashMap<String, SpotBugsIssue> {
        //Sonar Export

        var sonarIssues: List<SpotBugsIssue> = mutableListOf()

        //FIXME: Temporarly hardcoded value
        val connectionInfo = SonarConnectionInfo("http://localhost:9000/","sonar","sonar")

        try {
            sonarIssues = RemoteSonarSource(log, connectionInfo, client).getSonarIssues(groupdId, artifactId)
            log.info("Found ${sonarIssues.size} Sonar issues")
        } catch (ioe: IOException) {
            log.warn("Skipping sonar data import")
        }

        val sonarIssuesLookupTable = HashMap<String, SpotBugsIssue>()
        for (i in sonarIssues) {
            sonarIssuesLookupTable.put(i.getKey(), i)
        }
        return sonarIssuesLookupTable
    }

    fun getSpotBugsIssues(findbugsResults:File,classMappingFile: File): ArrayList<SpotBugsIssue> {

        if (!findbugsResults.exists()) {
            log.error("SpotBugs report (findbugsXml.xml) is missing")
            return ArrayList<SpotBugsIssue>()
        }
        if (!classMappingFile.exists()) {
            log.error("sonar/class_mapping.csv is missing")
            return ArrayList<SpotBugsIssue>()
        }

        return FindBugsReportSource(log).getSpotBugsIssues(FileInputStream(findbugsResults),FileInputStream(classMappingFile))
    }

    fun enrichSonarExportIssue(sonarIssuesLookupTable:HashMap<String, SpotBugsIssue>, spotBugsIssues:ArrayList<SpotBugsIssue>):ArrayList<SpotBugsIssue> {

        val exportedIssues = ArrayList<SpotBugsIssue>()

        if(sonarIssuesLookupTable.size > 0) {

            log.info("Found ${spotBugsIssues.size} SpotBugs issues")

            //Integrating SonarQube metadata
            for (sbIssue in spotBugsIssues) {

                var existingIssue = sonarIssuesLookupTable.get(sbIssue.getKey())
                if (existingIssue != null) {
                    existingIssue.cwe = sbIssue.cwe
                    existingIssue.methodSink = sbIssue.methodSink
                    existingIssue.methodSinkParameter = sbIssue.methodSinkParameter
                    existingIssue.unknownSource = sbIssue.unknownSource
                    existingIssue.sourceMethod = sbIssue.sourceMethod

                    exportedIssues.add(existingIssue)
                } else {
                    log.error("Unable to find the corresponding issue")
                }
            }
            return exportedIssues
        } else {

            log.warn("Using only SpotBugs as data source (${spotBugsIssues.size}) issues added")
            return spotBugsIssues
        }
    }

    fun graph(exportedIssues:ArrayList<SpotBugsIssue>,fileGraph:File) {
        //Integrating Neo4j metadata

        val db = GraphDatabaseFactory().newEmbeddedDatabase(fileGraph)
        try {
            val graphDb = Neo4jGraph(db)
            val totalIssue = exportedIssues.size
            var issueIndex = 0
            for (issue in exportedIssues) {
                issueIndex++
                if (issue.methodSink != "") {
                    val start = System.currentTimeMillis()

                    issue.hasTaintedSource = false
                    issue.hasSafeSource = false
                    issue.hasUnknownSource = false

                    if(issue.sourceMethod == null) {
                        log.warn("No source method defined for the entry : $issue")
                        continue
                    }
                    var nodes = graphDb.searchSource(issue.methodSink + "_p" + issue.methodSinkParameter, issue.sourceMethod!!)
                    for (n in nodes) {
                        when(n.state) {
                            "SAFE" -> {
                                issue.hasSafeSource = true
                            }
                            "TAINTED" -> {
                                issue.hasTaintedSource = true
                            }
                            "UNKNOWN" -> {
                                issue.hasUnknownSource = true
                            }
                            else -> {
                                log.warn("Unknown state : ${n.state}")
                            }
                        }
                    }


                    val end = System.currentTimeMillis()
                    log.info("Query executed ${end-start} ms (Tainted ${issue.hasTaintedSource}, Safe ${issue.hasSafeSource }, Unknown ${issue.hasUnknownSource})")
                }
                log.info("Issue %d - Progress %.2f %%".format(issueIndex, issueIndex * 100.0 / totalIssue))
            }
        }
        finally {
            db.shutdown();
        }

    }

    fun reportCoverage(exportedIssues:ArrayList<SpotBugsIssue>, spotBugsIssues: ArrayList<SpotBugsIssue>) {
        val pourcentCoverage = "%.2f".format((exportedIssues.size.toDouble() / spotBugsIssues.size.toDouble()) * 100.toDouble())
        val msg = "${exportedIssues.size} mapped issues from ${spotBugsIssues.size} total SB issues (${pourcentCoverage} %)"
        if((spotBugsIssues.size - exportedIssues.size) == 0) {
            log.info(msg)
        }
        else {
            log.warn(msg)
        }
    }

    fun exportCsv(exportedIssues: ArrayList<SpotBugsIssue>, aggregateResults: File) {
        if(exportedIssues.size > 0) {

            val writer = aggregateResults.printWriter()
            writer.println("SourceFile,LineNumber,GroupId,ArtifactId,Author,BugType,CWE,MethodSink,UnknownSource,SourceMethod,HasTainted Source,HasSafeSource,HasUnknownSource,Status,Key")
            for(finalIssue in exportedIssues) {
//                    var finalIssue = entry.value
                writer.println("${finalIssue.sourceFile},${finalIssue.startLine}," +
                        "${finalIssue.groupId},${finalIssue.artifactId}," +
                        "${finalIssue.author},${finalIssue.bugType},"+
                        "${finalIssue.cwe}," +
                        "${finalIssue.methodSink},${finalIssue.unknownSource}," +
                        "${finalIssue.sourceMethod},"+
                        "${finalIssue.hasTaintedSource?:""},${finalIssue.hasSafeSource?:""},${finalIssue.hasUnknownSource?:""}," +
                        "${finalIssue.status}," +
                        "${finalIssue.issueKey}")
            }

            writer.flush()
            writer.close()
        }
    }
}