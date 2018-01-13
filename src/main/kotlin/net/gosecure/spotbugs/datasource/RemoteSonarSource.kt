package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.SpotBugsIssue
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.json.JSONObject

class RemoteSonarSource(val log:Log, val hostUrl:String) {


    /**
     * Get a list of Sonar issues that include status from manual review.
     */
    fun getSonarIssues(project: MavenProject):List<SpotBugsIssue>  {

        val spotBugsIssues = ArrayList<SpotBugsIssue>()

        val projectKey = project.groupId + ":" + project.artifactId


        /*    val config = RequestConfig.custom()
                    .setProxy(HttpHost("127.0.0.1", 8080, "http"))
                    .build() */

        var n = 1
        var nbr_pages = 0
        while (n == 1 || n <= nbr_pages){
            val client = HttpClientBuilder.create().build()
            val uriIssues =  URIBuilder(hostUrl + "/api/issues/search")
                    .addParameter("componentKeys",projectKey)
                    .addParameter("ps", "500")
                    .addParameter("p",n.toString())
                    .build()
            log.info("URL Fetch: "+uriIssues)
            val get = HttpGet(uriIssues)
            //get.config = config

            val response = client.execute(get)

            val responseCode = response.statusLine.statusCode

            if(responseCode == 200) {
                val contentStream = response.entity.content

                val jsonObj = JSONObject(IOUtils.toString(contentStream,"UTF-8"))
                //log.info(jsonObj.toString())

                if (nbr_pages == 0) {
                    var total = jsonObj.getInt("total")
                    nbr_pages = total / 500 + 1
                }
                val issues = jsonObj.getJSONArray("issues")

                for (i in 0..(issues.length() - 1)) {
                    val issue = issues.getJSONObject(i)

                    try {
                        val issue_key = issue.getString("key")
                        val component = issue.getString("component")
                        val componentParts = component.split(":")
                        val groupId    = componentParts[0]
                        val artifactId = componentParts[1]
                        val sourceFile = componentParts[2]

                        val startLine = issue.getJSONObject("textRange")?.getInt("startLine")

                        var status = if(issue.has("status")) issue.getString("status") else ""

                        if (issue.has("resolution")) {
                            status = issue.getString("resolution")
                        }

                        var author = "unknown"
                        if (issue.has("author")) {
                            author = issue.getString("author")
                        }

                        val rule = issue.getString("rule").split(":")[1] //Trim the provider

                        //log.info("Found ${sourceFile} at line ${startLine} with the status ${status} caused by ${author}")

                        spotBugsIssues.add(SpotBugsIssue(sourceFile,
                                if (startLine == null) -1 else startLine,
                                groupId,
                                artifactId,
                                status, author, rule,
                                "", "", -1,"",issue_key)) //The last three values will be taken from spotbugs results..
                    }
                    catch(e:Exception){
                        log.error("Skipping element. Error occurs while parsing ${issue}",e)
                    }
                }
            }
            n++
        }
        return spotBugsIssues
    }
}