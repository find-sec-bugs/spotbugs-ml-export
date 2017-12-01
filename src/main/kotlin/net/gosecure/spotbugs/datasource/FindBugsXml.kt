package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.SpotBugsIssue
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.dom4j.io.SAXReader
import org.dom4j.tree.DefaultElement
import java.io.File

class FindBugsXml(val log: Log) {

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

        if (!findbugsResults.exists()) {
            log.error("sonar/findbugs-result.xml is missing")
            return spotBugsIssues
        }
        if (!classMappingFile.exists()) {
            log.error("sonar/class_mapping.csv is missing")
            return spotBugsIssues
        }

        val classMappingLoaded = getClassMapping(classMappingFile)

        //log.info("findbugs-result.xml: ${findbugsResults.exists()}")
        //log.info("class_mapping.csv  : ${classMappingFile.exists()}")

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

            val fullyQualifiedMethod = getMethodFile(elem)

            var issue = SpotBugsIssue(sourceFile.first,
                    sourceFile.second,
                    "","","","",
                    type, cweid , "", "","", fullyQualifiedMethod)

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

    private fun getMethodFile(elem: DefaultElement): String? {

        val methodNodes = elem.selectNodes("Method")
        for(meth in methodNodes) {
            val elem = meth as DefaultElement
            val isPrimary = elem.attribute("primary")?.value
            if(isPrimary == "true") {
                val className = elem.attribute("classname")?.value!!.replace('.','/')
                val name = elem.attribute("name")?.value
                val signature = elem.attribute("signature")?.value

                return "$className.$name$signature"
            }
        }

        return null
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

}