package net.gosecure.spotbugs

import net.gosecure.spotbugs.model.SpotBugsIssue
import org.mockito.Matchers
import org.mockito.Mockito.*
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset

class ExportLogicTest {

    @Test
    fun testOutput() {

        val export = ExportLogic(LogWrapper())

        //val fileOut = mock(File::class.java)
        val printWriter = mock(PrintWriter::class.java)
        //`when`(fileOut.printWriter(any<Charset>())).thenReturn(printWriter)

        val issues = ArrayList<SpotBugsIssue>()
        issues.add(SpotBugsIssue("test123.java",1,"net.gosecure.test","this-a-test",
                "GOOD","Philippe","SQL_INJECTION","89","execQuery...",0,
                "","","fetchUser",true,false, true))

        issues.add(SpotBugsIssue("test123.java",1,"net.gosecure.test","this-a-test",
                "GOOD","Philippe","SQL_INJECTION","",  "", -1, "", ""))

        export.exportCsv(issues,printWriter)

        assertEquals(issues.size,2)

        verify(printWriter, atLeast(1)).println(contains("SQL_INJECTION"))
        //Testing the empty values replaced (second entry)
        verify(printWriter, atLeast(1)).println(contains("NO_CWE"))
        verify(printWriter, atLeast(1)).println(contains("NO_SOURCE_METHOD"))
    }
}