package httpclient.testutil

import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod

abstract class BaseHttpClientTestCase {

    protected  val httpClient = Mockito.mock(HttpClient::class.java)
    val httpGet = Mockito.mock(HttpGet::class.java)
    val httpResponse = Mockito.mock(HttpResponse::class.java)
    val statusLine = Mockito.mock(StatusLine::class.java)


    @BeforeMethod
    fun setUp() {
        //General behavior
        `when`(statusLine.getStatusCode()).thenReturn(500)
        `when`(httpResponse.getStatusLine()).thenReturn(statusLine)
        `when`(httpClient.execute(Matchers.any())).thenReturn(httpResponse)

    }
}