package httpclient.testutil.matcher

import org.apache.http.client.methods.HttpUriRequest
import org.mockito.Matchers

class RequestMatcherBuilder(val path:String?=null,val query:String?=null) {

    fun build(): HttpUriRequest? {
        return Matchers.argThat(RequestMatcher(path, query))
    }
}