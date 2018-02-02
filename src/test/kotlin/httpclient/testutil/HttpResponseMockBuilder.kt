package httpclient.testutil

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.entity.InputStreamEntity
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class HttpResponseMockBuilder(val contentPathResponse:String="", val contentResponse:String="", val statusCode:Int=200) {

    fun build():HttpResponse {
        val resp = mock(HttpResponse::class.java)

        `when`(resp.statusLine).thenReturn(BasicStatusLine(HttpVersion.HTTP_1_1,
                if (statusCode!=null) statusCode else 200,
                "OK"))

        if(contentPathResponse != null) {
            val stream = this.javaClass.getResourceAsStream(contentPathResponse)
            if(stream == null) {
                println("Unable to find mock response ${contentPathResponse}")
            }
            else {
                `when`(resp.entity).thenAnswer({
                    println("Returning file $contentPathResponse")
                    return@thenAnswer InputStreamEntity(stream) })
            }
        }
        else if(contentResponse != null) {
            `when`(resp.entity).thenAnswer({
                println("Returning $contentResponse")
                return@thenAnswer StringEntity(contentResponse) })
        }
        else {
            `when`(resp.entity).thenReturn(StringEntity(""))
        }

        return resp
    }
}
