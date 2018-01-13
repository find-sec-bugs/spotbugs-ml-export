package net.gosecure.spotbugs


/**
 * Data structure that combine metadata from Sonar and SpotBugs report.
 *
 * @param sourceFile Source file where the bug was found
 * @param startLine  Line number of the bug (Note: the bug could be on multiple lines)
 * @param groupId    GroudId where the bug was found
 * @param artifactId ArtifactId where the bug was found (Linked to the groupId)
 * @param status     The status given to the bug based on human interaction
 * @param author     Author of the line (This is analog to Git blame)
 * @param bugType    Constant that identify uniquely bug type, ie: SQL_INJECTION_ANDROID
 * @param cwe        Common Weakness Enumeration
 * @param methodSink API used that is potentially
 * @param unknownSource Unknown method from which the return value is passed to a sensible API
 * @param issueKey   Key for SonarQube
 * @param sourceMethod Method in witch the bug was found. ()
 * @param hasTaintedSource One taint variable can reach the API
 * @param hasSafeSource    One safe variable can reach the API
 * @param hashAllSources   Hash representation of all the sources
 */
class SpotBugsIssue(var sourceFile:String,
                    var startLine:Int,
                    var groupId:String,
                    var artifactId:String,
                    var status:String,
                    var author:String,
                    var bugType: String,
                    var cwe:String?,
                    var methodSink:String,
                    var methodSinkParameter:Int,
                    var unknownSource:String,
                    var issueKey:String,
                    var sourceMethod:String? = null,
                    var hasTaintedSource:Boolean? = null,
                    var hasSafeSource:Boolean? = null,
                    var hasUnknownSource:Boolean? = null,
                    var hashAllSources:String = "") {

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