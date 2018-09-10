package net.gosecure.spotbugs.model

class SonarUtil {

    /**
     * The source file from the SpotBugs issue might not match exactly the one from Sonar.
     * (Sonar path include source directory.)
     *
     */
    fun getIssueFromLookupTable(sonarIssuesLookupTable:HashMap<String, SpotBugsIssue>,spotBugsIssue: SpotBugsIssue):SpotBugsIssue? {
        for(potentialSourceDirectory in arrayOf("src","src/main/java")) {
            val res = sonarIssuesLookupTable.get(potentialSourceDirectory+"/"+spotBugsIssue.getKey())
            if(res != null) return res
        }

//        for(e in sonarIssuesLookupTable.toMap().entries) {
//            val lastSlash = spotBugsIssue.sourceFile.lastIndexOf('/')
//            if(e.value.sourceFile.endsWith(spotBugsIssue.sourceFile)) {
//                if(e.value.startLine == spotBugsIssue.startLine && e.value.bugType == spotBugsIssue.bugType) {
//                    return e.value
//                }
//            }
//        }
        return null
    }
}