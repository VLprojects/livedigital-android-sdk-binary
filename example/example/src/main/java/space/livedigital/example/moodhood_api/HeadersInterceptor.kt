package space.livedigital.example.moodhood_api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class HeadersInterceptor : Interceptor {
    private var clientId: String? = null
    private var accessToken: String? = null
    private var tokenType: String? = null

    fun setClientId(clientId: String?) {
        this.clientId = clientId
    }

    fun setAccessToken(tokenType: String?, accessToken: String?) {
        this.tokenType = tokenType
        this.accessToken = accessToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = HeadersRequestBuilder(chain.request()).apply {
            addAuthToken(tokenType, accessToken)
            addClientId(clientId)
        }.build()
        return chain.proceed(request)
    }
}

private class HeadersRequestBuilder(request: Request) {
    private var requestBuilder = request.newBuilder()

    fun addClientId(clientId: String?): HeadersRequestBuilder {
        clientId?.let { requestBuilder.addHeader(CLIENT_ID, it) }
        return this
    }

    fun addAuthToken(tokenType: String?, authToken: String?): HeadersRequestBuilder {
        if (tokenType != null && authToken != null) {
            requestBuilder.addHeader(AUTHORIZATION, "${tokenType} $authToken")
        }
        return this
    }

    fun build(): Request {
        return requestBuilder.build()
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val CLIENT_ID = "x-cl-id"
    }
}