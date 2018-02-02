package net.gosecure.spotbugs.datasource

import org.testng.Assert
import org.testng.annotations.Test

class FindBugsReportSourceTest {


    @Test
    fun testIssuesParsing() {

        val spotbugsReport = this.javaClass.getResourceAsStream("/findbugs/reports/struts_core.xml")
        val classMappingFile = this.javaClass.getResourceAsStream("/findbugs/reports/struts_mapping.csv")

        val spotbugsIssues = FindBugsReportSource(DummyLogWrapper()).getSpotBugsIssues(spotbugsReport,classMappingFile)

        Assert.assertEquals(spotbugsIssues.size, 24) // 24 BugInstance in struts_core.xml

    }
}