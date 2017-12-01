package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.datasource.graph.VariableNode
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Result
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import java.io.File

class Neo4jGraph(val graphDb:GraphDatabaseService) {

    fun searchTaintSource(methodApi:String, source:String? = null, sourceType:String="TAINTED") : Set<VariableNode> {
        val tx = graphDb.beginTx()
        try {

            var execResult:Result?
            if(source == null) {
                execResult = queryGraph("""
MATCH (source:Variable)-[r1:TRANSFER*1..5]->(sink:Variable)
WHERE source.state = ${"$"}sourceType AND
sink.name = ${"$"}methodApi
RETURN source,sink;
""".trim(), hashMapOf("methodApi" to methodApi, "sourceType" to sourceType), graphDb)

            }
            else {
                execResult = queryGraph("""
MATCH (source:Variable)-[r1:TRANSFER*0..5]->(node:Variable)-[r:TRANSFER]->(sink:Variable)
WHERE source.state = ${"$"}sourceType AND
sink.name = ${"$"}methodApi
AND r.source = ${"$"}source
RETURN source,sink,r1,node,r;
""".trim(), hashMapOf("methodApi" to methodApi, "source" to source, "sourceType" to sourceType), graphDb)

            }


            val listNode = HashSet<VariableNode>()

            for (row in execResult) {
                var sourceNode = row.get("source") as Node
                listNode.add(VariableNode(sourceNode.getProperty("name").toString()))
            }

            tx.success()
            return listNode
        } finally {
            tx.close()
        }
    }

    fun queryGraph(query:String, params:Map<String,String>, graphDb: GraphDatabaseService) : Result {

        val queryNoSpace = query.replace(Regex.fromLiteral("[\n\r]"),"")
        //println("Executing query $queryNoSpace with $params")

        val execResult = graphDb.execute(query, params)

        return execResult
    }


}