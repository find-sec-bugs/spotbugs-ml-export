package net.gosecure.spotbugs

import org.apache.commons.io.IOUtils
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.json.JSONObject
import java.io.File
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.config.RequestConfig
import org.apache.http.HttpHost
import org.dom4j.io.SAXReader
import org.dom4j.tree.DefaultElement

@Mojo(name="export-csv")
class ExportMojo : AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private val project: MavenProject? = null

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    override fun execute() {
        log.info("Hello World from Kotlin Maven plugin !")

        //var isRootPom = project!!.isExecutionRoot()

        val sonarIssues = getSonarIssues(project!!)
        log.info("Found ${sonarIssues.size} Sonar issues")

        val sonarIssuesLookupTable = HashMap<String,SpotBugsIssue>()
        for(i in sonarIssues) {
            sonarIssuesLookupTable.put(i.getKey(), i)
        }

        if(sonarIssuesLookupTable.size > 0) {

            var spotBugsIssues = getSpotBugsIssues(project!!)

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
                log.info("${exportedIssues.size} mapped issues from ${spotBugsIssues.size} total SB issues (${pourcentCoverage} %)")

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
                            "${finalIssue.status},${finalIssue.issue_key}")
                }

                writer.flush()
                writer.close()
            }

        }
    }

    /**
     * Get a list of SpotBugs issues
     */
    fun getSpotBugsIssues(project: MavenProject):List<SpotBugsIssue> {
        val spotBugsIssues = ArrayList<SpotBugsIssue>()

        val buildDir = project!!.build.directory
        val sonarDir = File(buildDir, "sonar")

        if (!sonarDir.exists()) {
            log.warn("No sonar directory found in the project ${project!!.basedir}. Sonar must be runned prior to the export.")
            return spotBugsIssues
        }

        val findbugsResults = File(sonarDir, "findbugs-result.xml")
        val classMappingFile = File(sonarDir, "class-mapping.csv")
        val classMappingLoaded = getClassMapping(classMappingFile)

        if (!findbugsResults.exists()) {
            log.error("sonar/findbugs-result.xml is missing")
            return spotBugsIssues
        }
        if (!classMappingFile.exists()) {
            log.error("sonar/class_mapping.csv is missing")
            return spotBugsIssues
        }

//        log.info("findbugs-result.xml: ${findbugsResults.exists()}")
//        log.info("class_mapping.csv  : ${classMappingFile.exists()}")

        val reader = SAXReader()
        val document = reader.read(findbugsResults)
        val bugInstances = document.getRootElement().selectNodes("BugInstance")

        for (bug in bugInstances) {

            val elem = bug as DefaultElement
            val type = elem.attribute("type").value
            var cweid = elem.attribute("cweid")?.value
            var instanceHash = elem.attribute("instanceHash").value
            if(cweid == null) cweid = ""

            val sourceClass = getLineOfCode(elem)
            if(sourceClass == null) {
                log.warn("BugInstance has no start line of code  ($instanceHash)")
                continue
            }
            val sourceFile = getSourceFile(sourceClass.first,sourceClass.second, classMappingLoaded)
            if(sourceFile == null) {
                log.warn("Unable to map the class ${sourceClass.first}:${sourceClass.second}")
                continue
            }

            var issue = SpotBugsIssue(sourceFile.first,
                    sourceFile.second,
                    "","","","",
                    type, cweid , "", "","")

            for(stringValue in elem.selectNodes("String")) { //Extra properties
                val stringElem = stringValue as DefaultElement

                if(stringElem.attribute("role")?.value == "Sink method") {
                    val methodSink = stringElem.attribute("value").value
                    issue.methodSink = methodSink
                }
                if(stringElem.attribute("role")?.value == "Unknown source") {
                    val unknownSource = stringElem.attribute("value").value
                    issue.unknownSource = unknownSource
                }
            }

            spotBugsIssues.add(issue)
        }

        return spotBugsIssues
    }

    fun getLineOfCode(elem:DefaultElement):Pair<String,Int>? {
        val classNodes = elem.selectNodes("Class")
        val methodNodes = elem.selectNodes("Method")
        val fieldNodes = elem.selectNodes("Field")
        val sourceLineNodes  = elem.selectNodes("SourceLine")

        var elem:DefaultElement? = null
        if(sourceLineNodes.size>0) {
            elem = sourceLineNodes[0] as DefaultElement
        }
        else if(methodNodes.size>0) {
            elem = methodNodes[0].selectNodes("SourceLine")[0] as DefaultElement
        }
        else if(classNodes.size>0) {
            elem = classNodes[0].selectNodes("SourceLine")[0] as DefaultElement
        }

        if(elem != null) {
            val className = elem.attribute("classname")?.value
            val line      = elem.attribute("start")?.value
            if (className != null && line != null) {
                return Pair(className, Integer.parseInt(line))
            }
        }

        return null
    }

    fun getClassMapping(classMappingFile: File):Map<Pair<String,Int>,Pair<String,Int>> {
        val mapping = HashMap<Pair<String,Int>,Pair<String,Int>>()

        for(line in classMappingFile.readLines()) {
            val parts = line.split(",")
            if(parts.size < 2) continue
            var classPart = parts[0].split(":")
            var filePart = parts[1].split(":")
            if(classPart.size < 2 || filePart.size < 2) {
                log.error("The Mapping is not properly formatted ($line)")
                continue
            }
            if(mapping.get(Pair(classPart[0],Integer.parseInt(classPart[1]))) != null) continue
            mapping.put(Pair(classPart[0],Integer.parseInt(classPart[1])), Pair(filePart[0],Integer.parseInt(filePart[1])))
        }
        return mapping
    }

    fun getSourceFile(className: String, line: Int, classMappingLoaded: Map<Pair<String, Int>, Pair<String, Int>>):Pair<String,Int>? {
        return classMappingLoaded.get(Pair(className, line))
    }

    /**
     * Get a list of Sonar issues that include status from manual review.
     */
    fun getSonarIssues(project:MavenProject):List<SpotBugsIssue>  {

        val spotBugsIssues = ArrayList<SpotBugsIssue>()

        val projectKey = project.groupId + ":" + project.artifactId


    /*    val config = RequestConfig.custom()
                .setProxy(HttpHost("127.0.0.1", 8080, "http"))
                .build() */

        var n = 1
        var nbr_pages = 0
        while (n == 1 || n <= nbr_pages){
            val client = HttpClientBuilder.create().build()
            val get = HttpGet(URIBuilder("http://localhost:9000/api/issues/search")
                    .addParameter("componentKeys",projectKey)
                    .addParameter("ps", "500")
                    .addParameter("p",n.toString())
                    .build())
            //get.config = config

            val response = client.execute(get)

            val responseCode = response.statusLine.statusCode

            if(responseCode == 200) {
                val contentStream = response.entity.content

                val jsonObj = JSONObject(IOUtils.toString(contentStream,"UTF-8"))
                log.info(jsonObj.toString())

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

                        if (status == "RESOLVED" && issue.has("resolution") != null) {
                            status = issue.getString("resolution")
                        }

                        var author = "unknown"
                        if (issue.has("author") != null) {
                            author = issue.getString("author")
                        }

                        val rule = issue.getString("rule").split(":")[1] //Trim the provider

                        log.info("Found ${sourceFile} at line ${startLine} with the status ${status} caused by ${author}")

                        spotBugsIssues.add(SpotBugsIssue(sourceFile,
                                if (startLine == null) -1 else startLine,
                                groupId,
                                artifactId,
                                status, author, rule,
                                "", "", "",issue_key)) //The last three values will be taken from spotbugs results..
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