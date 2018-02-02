package httpclient.testutil.matcher

import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

class RequestMatcher(val path:String?, val query:String?) : BaseMatcher<HttpUriRequest>() {

    override fun describeTo(description: Description?) {
        if(description != null) {
            description.appendText("HttpRequest:\n")

            if(path != null) {
                description.appendText("path=").appendText(path)
            }
            if(query != null) {
                description.appendText("query=").appendText(query)
            }
        }
    }

    override fun matches(req: Any?): Boolean {
        if(req is HttpRequestBase) {

            var criteriaMatches = true

            if(path != null) {
                criteriaMatches = criteriaMatches && req.uri.path.equals(path)
            }
            if(query != null) {
                criteriaMatches = criteriaMatches && req.uri.query.toString().equals(query)
            }

            return criteriaMatches
        }
        return false
    }

}