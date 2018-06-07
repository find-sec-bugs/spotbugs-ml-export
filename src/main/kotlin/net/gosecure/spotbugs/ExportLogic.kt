package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.FindBugsReportSource
import net.gosecure.spotbugs.datasource.GraphSource
import net.gosecure.spotbugs.datasource.RemoteSonarSource
import net.gosecure.spotbugs.model.SonarConnectionInfo
import net.gosecure.spotbugs.model.SpotBugsIssue
import net.gosecure.spotbugs.sourcemapper.SourceCodeMapper
import org.apache.http.client.HttpClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter

/**
 * This class should be agnostic of platform. It will include the logic reuse by the CLI and Maven integration.
 */
class ExportLogic(var log:LogWrapper) {

    fun getSonarIssues(groupdId: String, artifactId: String, client: HttpClient): HashMap<String, SpotBugsIssue> {
        //Sonar Export

        var sonarIssues: List<SpotBugsIssue> = mutableListOf()

        //FIXME: Temporarily hardcoded value
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

    fun getSpotBugsIssues(findbugsResults:File,sourceCodeMapper: SourceCodeMapper): ArrayList<SpotBugsIssue> {

        if (!findbugsResults.exists()) {
            log.error("SpotBugs report (findbugsXml.xml) is missing")
            return ArrayList<SpotBugsIssue>()
        }

        return FindBugsReportSource(log).getSpotBugsIssues(FileInputStream(findbugsResults),sourceCodeMapper)
    }

    fun updateArtifactIdForAllIssues(issues:ArrayList<SpotBugsIssue> ,groupId:String, artifactId:String) {
        if(groupId == "" && artifactId == "") {
            return;
        }
        for(issue in issues) { // All issues will inherits the groupId and artifactId from the current project
            issue.groupId    = groupId
            issue.artifactId = artifactId
        }
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

    fun exportCsv(exportedIssues: ArrayList<SpotBugsIssue>, writer: PrintWriter) {
        if(exportedIssues.size > 0) {

            //val writer = csvFile.printWriter()
            writer.println("m#SourceFile,D#GroupId,D#ArtifactId,D#Author,D#BugType,D#CWE,D#MethodSink," +
                    "D#UnknownSource,D#SourceMethod,D#HasTaintedSource,D#HasSafeSource,D#HasUnknownSource,D#Status,m#Key")
            for(finalIssue in exportedIssues) {
//                    var finalIssue = entry.value
                writer.println("${finalIssue.sourceFile}:${finalIssue.startLine}," +
                        "${finalIssue.groupId},${finalIssue.artifactId}," +
                        "${notEmpty(finalIssue.author,"UNKNOWN_AUTHOR")},${finalIssue.bugType},"+
                        "${notEmpty(finalIssue.cwe,"NO_CWE")}," +
                        "${notEmpty(finalIssue.methodSink,"NO_METHOD_SINK")},${notEmpty(finalIssue.unknownSource,"NO_UNKNOWN_SOURCE")}," +
                        "${notEmpty(finalIssue.sourceMethod,"NO_SOURCE_METHOD")},"+
                        "${notEmpty(finalIssue.hasTaintedSource,"NOT_APP")},${finalIssue.hasSafeSource?:"NOT_APP"},${finalIssue.hasUnknownSource?:"NOT_APP"}," +
                        "${finalIssue.status?:""}," +
                        "${finalIssue.issueKey?:""}")
            }

            writer.flush()
            writer.close()
        }
    }

    fun notEmpty(value:Any?,default:String):String {
        if(value ==null) return default
        if(value is String && value == "") return default
        else
            return if (value is Boolean) ""+value
                else if(value is String) value
                else ""
    }


}