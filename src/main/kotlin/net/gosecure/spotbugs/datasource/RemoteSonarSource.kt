package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.LogWrapper
import net.gosecure.spotbugs.model.SonarConnectionInfo
import net.gosecure.spotbugs.model.SpotBugsIssue
import org.apache.commons.io.IOUtils
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.json.JSONObject
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpParams



class RemoteSonarSource(val log: LogWrapper, val connectionInfo: SonarConnectionInfo, val httpClient: HttpClient) {


    /**
     * Get a list of Sonar issues that include status from manual review.
     */
    fun getSonarIssues(groupId:String, artifactId:String):List<SpotBugsIssue>  {

        val spotBugsIssues = ArrayList<SpotBugsIssue>()

        val projectKey = groupId + ":" + artifactId


        /*    val config = RequestConfig.custom()
                    .setProxy(HttpHost("127.0.0.1", 8080, "http"))
                    .build() */

        val httpParams = BasicHttpParams()
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000)

        var n = 1
        var nbr_pages = 0
        while (n == 1 || n <= nbr_pages){
            val uriIssues =  URIBuilder(connectionInfo.url + "/api/issues/search")
                    .addParameter("componentKeys",projectKey)
                    .addParameter("ps", "500")
                    .addParameter("p", n.toString())
                    .build()
            log.info("URL Fetch: "+uriIssues)
            val get = HttpGet(uriIssues)


            val response = httpClient.execute(get)

            val responseCode = response.statusLine.statusCode

            if(responseCode == 200) {
                response.entity.content.use { contentStream ->


                    val jsonString = IOUtils.toString(contentStream, "UTF-8")
                    val jsonObj = JSONObject(jsonString)
                    //log.info(jsonObj.toString())

                    if (nbr_pages == 0) {
                        var total = jsonObj.getInt("total")
                        nbr_pages = total / 500 + 1
                    }
                    val issues = jsonObj.getJSONArray("issues")

                    for (i in 0..(issues.length() - 1)) {
                        val issue = issues.getJSONObject(i)

                        try {
                            val rule = issue.getString("rule")
                            if (!rule.startsWith("findbugs") && !rule.startsWith("findsecbugs")) {
                                continue
                            }

                            val issue_key = issue.getString("key")
                            val component = issue.getString("component")
                            val componentParts = component.split(":")
                            val groupId = componentParts[0]
                            val artifactId = componentParts[1]
                            val sourceFile = componentParts[2]

                            val startLine = if (issue.has("textRange")) {
                                issue.getJSONObject("textRange")?.getInt("startLine")
                            } else {
                                -1
                            }

                            var status = if (issue.has("status")) issue.getString("status") else ""

                            if (issue.has("resolution")) {
                                status = issue.getString("resolution")
                            }

                            var author = "unknown"
                            if (issue.has("author")) {
                                author = issue.getString("author")
                            }

                            val ruleSuffix = rule.split(":")[1] //Trim the provider

                            //log.info("Found ${sourceFile} at line ${startLine} with the status ${status} caused by ${author}")

                            spotBugsIssues.add(SpotBugsIssue(sourceFile,
                                    if (startLine == null) -1 else startLine,
                                    groupId,
                                    artifactId,
                                    status, author, ruleSuffix,
                                    "", "", -1, "", issue_key)) //The last three values will be taken from spotbugs results..
                        } catch (e: Exception) {
                            log.error("Skipping element. Error occurs while parsing ${issue}", e)
                        }
                    }
                }
            }
            n++
        }
        return spotBugsIssues
    }
}