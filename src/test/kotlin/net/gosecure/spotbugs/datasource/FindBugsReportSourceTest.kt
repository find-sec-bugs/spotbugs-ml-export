package net.gosecure.spotbugs.datasource

import net.gosecure.spotbugs.sourcemapper.FileSourceCodeMapper
import org.testng.Assert
import org.testng.annotations.Test

class FindBugsReportSourceTest {


    @Test
    fun testIssuesParsing() {

        val spotbugsReport = this.javaClass.getResourceAsStream("/findbugs/reports/struts_core.xml")
        val classMappingFile = this.javaClass.getResourceAsStream("/findbugs/reports/struts_mapping.csv")

        val log = DummyLogWrapper()

        val spotbugsIssues = FindBugsReportSource(log).getSpotBugsIssues(spotbugsReport, FileSourceCodeMapper(classMappingFile,log))

        Assert.assertEquals(spotbugsIssues.size, 24) // 24 BugInstance in struts_core.xml

    }
}