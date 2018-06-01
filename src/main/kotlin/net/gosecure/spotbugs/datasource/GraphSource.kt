package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.LogWrapper
import net.gosecure.spotbugs.datasource.graph.Neo4jGraph
import net.gosecure.spotbugs.model.SpotBugsIssue
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import java.io.File

class GraphSource(val log: LogWrapper) {

    fun addGraphMetadata(exportedIssues:ArrayList<SpotBugsIssue>, fileGraph: File) {
        val db = GraphDatabaseFactory().newEmbeddedDatabase(fileGraph)
        try {
            val graphDb = Neo4jGraph(db)
            val totalIssue = exportedIssues.size
            var issueIndex = 0
            for (issue in exportedIssues) {
                issueIndex++
                if (issue.methodSink != "") {
                    val start = System.currentTimeMillis()

                    //For many bugs graph queries will not applied. It is important to do the distinction between bugs
                    issue.hasTaintedSource = false
                    issue.hasSafeSource = false
                    issue.hasUnknownSource = false

                    if (issue.sourceMethod == null) {
                        //log.warn("No source method defined for the entry : $issue")
                        continue
                    }
                    var nodes = graphDb.searchSource(issue.methodSink + "_p" + issue.methodSinkParameter, issue.sourceMethod!!)
                    for (n in nodes) {
                        when (n.state) {
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
                    log.info("Query executed ${end - start} ms (Tainted ${issue.hasTaintedSource}, Safe ${issue.hasSafeSource}, Unknown ${issue.hasUnknownSource})")
                }
                log.info("Issue %d - Progress %.2f %%".format(issueIndex, issueIndex * 100.0 / totalIssue))
            }
        } finally {
            db.shutdown();
        }
    }
}
