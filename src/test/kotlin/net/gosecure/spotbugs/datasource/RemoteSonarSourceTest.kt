package net.gosecure.spotbugs.datasource

import httpclient.testutil.BaseHttpClientTestCase
import httpclient.testutil.HttpResponseMockBuilder
import httpclient.testutil.matcher.RequestMatcherBuilder
import net.gosecure.spotbugs.ExportLogic
import org.testng.annotations.Test
import org.mockito.Mockito.*
import org.testng.Assert.assertEquals


class RemoteSonarSourceTest : BaseHttpClientTestCase() {

    @Test
    fun testIssuesParsing() {
        val logic = ExportLogic(DummyLogWrapper(),httpClient)

        //Mock response to parse
        val resp = HttpResponseMockBuilder("/sonar/responses/1.json").build()
        `when`(httpClient.execute(RequestMatcherBuilder("/api/issues/search").build()))
                .thenReturn(resp)

        val sonarIssues = logic.getSonarIssues("com.request", "request")

        assertEquals(sonarIssues.size,163)

        verify(httpClient).execute(RequestMatcherBuilder("/api/issues/search").build())
    }
}