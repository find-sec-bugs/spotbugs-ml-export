package net.gosecure.spotbugs.datasource

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.testng.annotations.Test
import java.io.File
import org.neo4j.graphdb.factory.GraphDatabaseSettings
import java.nio.charset.Charset


class Neo4jGraphTest {

    @Test(enabled = false)
    fun simpleQueryGraph() {

        val basePath = "C:\\Tools\\findsecbugs-cli-1.7.1"
        //val target = "struts.db";
        //val target = "$basePath\\dropwizard.db"
        //val target = "$basePath\\wicket.db"
        val target = "C:\\Code\\samples\\struts\\codegraph.db"

        val graphDb = GraphDatabaseFactory().newEmbeddedDatabase( File(target) )
        Runtime.getRuntime().addShutdownHook(Thread { graphDb.shutdown() })

        println("Database loaded")

        val sinksDir = File("C:\\Code\\projects\\find-sec-bugs-graph\\plugin\\src\\main\\resources\\injection-sinks\\")
        val sinksFiles = sinksDir.listFiles()
        val sinksFilesInclude = listOf("beans.txt","command.txt","command-scala.txt","el.txt","ldap.txt","path-traversal-in.txt",
                "path-traversal-out.txt","script-engine.txt","seam-el.txt","spel.txt","sql-hibernate.txt",
                "sql-jdbc.txt","sql-jdo.txt","sql-jpa.txt","sql-spring.txt","sql-turbine.txt","struts2.txt","xslt.txt")

        val sinksApi = ArrayList<String>()
        for(f in sinksFiles) {
            //println("Sink file ${f.getName()} loaded")
            if(!sinksFilesInclude.contains(f.getName())) continue

            val lines = f.readLines(Charset.forName("UTF-8"))
            for (line in lines) {
                if(line.trim() == "") continue
                if(line[0] == '-') continue

                sinksApi.add(line.trim())
            }
        }
        for(sink in sinksApi) {

            for(nodeName in sinkToNodeNames(sink)) {
                //println(nodeName)
                val nodes = Neo4jGraph(graphDb).searchTaintSource(nodeName)

                if (nodes.isNotEmpty()) {
                    println("Source found for ${nodeName}")
                }
                for (n in nodes) {
                    println(" - ${n.name}")
                }
//                if (nodes.size == 0) {
//                    println("No vulnerability found")
//                }
            }
        }
    }

    fun sinkToNodeNames(sink:String):List<String> {
        val parts = sink.split(":")
        val indexes = parts[1].split(",")

        val allNodes = ArrayList<String>()
        for(i in indexes) {
            allNodes.add("${parts[0]}_p${i}")
        }
        return allNodes
    }
}