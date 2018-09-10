package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.LogWrapper
import net.gosecure.spotbugs.model.SpotBugsIssue
import net.gosecure.spotbugs.sourcemapper.SourceCodeMapper
import org.apache.maven.plugin.logging.Log
import org.dom4j.Node
import org.dom4j.io.SAXReader
import org.dom4j.tree.DefaultElement
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

class FindBugsReportSource(val log: LogWrapper) {

    /**
     * Get a list of SpotBugs issues
     */
    fun getSpotBugsIssues(findbugsResults:InputStream,classMapper: SourceCodeMapper):ArrayList<SpotBugsIssue> {
        val spotBugsIssues = ArrayList<SpotBugsIssue>()

        //log.info("findbugs-result.xml: ${findbugsResults.exists()}")
        //log.info("class_mapping.csv  : ${classMappingFile.exists()}")

        val reader = SAXReader()
        val document = reader.read(findbugsResults)
        val bugInstances = document.rootElement.selectNodes("BugInstance")

        for (bug in bugInstances) {

            val elem = bug as DefaultElement
            val type = elem.attribute("type").value
            var cweid = elem.attribute("cweid")?.value
            var instanceHash = elem.attribute("instanceHash")?.value
            if(cweid == null) cweid = ""

            val sourceClass = getLineOfCode(elem)
            if(sourceClass == null) {
                log.warn("BugInstance has no start line of code  ($instanceHash)")
                continue
            }
            var sourceFile = classMapper.getSourceFile(sourceClass.first,sourceClass.second)
            if(sourceFile == null) {
                log.warn("Unable to map the class ${sourceClass.first}:${sourceClass.second}")
                //continue
                sourceFile = Pair("",1)
            }

            val fullyQualifiedMethod = getMethodFile(elem)

            var issue = SpotBugsIssue(sourceFile.first,
                    sourceFile.second,
                    "", "", "", "",
                    type, cweid, "", -1, "", "", fullyQualifiedMethod)

            for(stringValue in elem.selectNodes("String")) { //Extra properties
                val stringElem = stringValue as DefaultElement

                if(stringElem.attribute("role")?.value == "Sink method") {
                    val methodSink = stringElem.attribute("value").value
                    issue.methodSink = methodSink
                }
                if(stringElem.attribute("role")?.value == "Sink parameter") {
                    val methodSinkParam = stringElem.attribute("value").value
                    issue.methodSinkParameter = Integer.parseInt(methodSinkParam)
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
            //val isPrimary = elem.attribute("primary")?.value
            //if(isPrimary == "true")

            val className = elem.attribute("classname")?.value!!.replace('.','/')
            val name = elem.attribute("name")?.value
            val signature = elem.attribute("signature")?.value

            return "$className.$name$signature"
        }

        return null
    }


    fun getLineOfCode(elem:DefaultElement):Pair<String,Int>? {
        val classNodes  = getChild("Class" ,elem)
        val methodNodes = getChild("Method",elem)
        val fieldNodes  = getChild("Field" ,elem)
        val sourceLineNodes = getChild("SourceLine",elem)

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

    fun getChild(nodeName:String, elem:DefaultElement):List<Node> {
        val nodes = ArrayList<Node>()
        for(e in elem.elements()) {
            if(e.name == nodeName) {
                nodes.add(e)
            }
        }
        return nodes
    }


}