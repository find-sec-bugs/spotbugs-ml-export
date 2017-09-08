package net.gosecure.spotbugs

/**
 * Data structure that combine metadata from Sonar and SpotBugs report.
 */
class SpotBugsIssue(var sourceFile:String,
                         var startLine:Int,
                         var groupId:String,
                         var artifactId:String,
                         var status:String,
                         var author:String,
                         var bugType: String,
                         var cwe:String,
                         var methodSink:String,
                         var unknownSource:String) {

    fun getKey():String {
        return Companion.getKey(sourceFile,startLine,bugType)
    }

    companion object {
        fun getKey(sourceFile: String, startLine: Int, bugType:String):String
        {
            return "$sourceFile:$startLine:$bugType"
        }
    }
}