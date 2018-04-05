package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.FindBugsReportSource
import net.gosecure.spotbugs.datasource.GraphSource
import net.gosecure.spotbugs.datasource.RemoteSonarSource
import net.gosecure.spotbugs.model.SonarConnectionInfo
import net.gosecure.spotbugs.model.SpotBugsIssue
import org.apache.http.client.HttpClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * This class should be agnostic of platform. It will include the logic reuse by the CLI and Maven integration.
 */
class ExportLogic(var log:LogWrapper) {

    fun getSonarIssues(groupdId: String, artifactId: String, client: HttpClient): HashMap<String, SpotBugsIssue> {
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
        GraphSource(log).addGraphMetadata(exportedIssues,fileGraph)


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

    fun exportCsv(exportedIssues: ArrayList<SpotBugsIssue>, csvFile: File) {
        if(exportedIssues.size > 0) {

            val writer = csvFile.printWriter()
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