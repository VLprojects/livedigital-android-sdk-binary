package space.livedigital.example.calls.utils

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OutboundCallTokenGenerator(
    private val devicesApiKey: String,
    private val signalingTokenKey: String,
    private val deviceId: String,
) {

    fun makeOutboundCallToken(
        callerPhoneNumber: String,
        calleePhoneNumber: String,
    ): String {
        val tenantId = devicesApiKey.substringBefore(':')
        val nowSeconds = System.currentTimeMillis() / 1000

        val header = JSONObject()
            .put("alg", "HS256")
            .put("typ", "JWT")

        val payload = JSONObject()
            .put("iss", tenantId)
            .put("sub", deviceId)
            .put("aud", "host")
            .put("exp", nowSeconds + TOKEN_TTL_SECONDS)
            .put("iat", nowSeconds - IAT_CLOCK_SKEW_SECONDS)
            .put("jti", UUID.randomUUID().toString())
            .put("channelId", callerPhoneNumber)
            .put("groups", JSONArray(listOf("user")))
            .put("producePermissions", JSONArray(listOf("microphone")))
            .put(
                "externalCall",
                JSONObject()
                    .put("direction", "outbound")
                    .put("callee", calleePhoneNumber)
                    .put("tenantId", tenantId)
                    .put("caller", callerPhoneNumber)
            )

        val signingInput = "${header.toBase64Url()}.${payload.toBase64Url()}"
        return "$signingInput.${sign(signingInput, signalingTokenKey)}"
    }

    private fun JSONObject.toBase64Url(): String = toString().toByteArray().toBase64Url()

    private fun ByteArray.toBase64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(this)

    private fun sign(input: String, secret: String): String =
        Mac.getInstance(HMAC_ALGORITHM)
            .apply {
                init(SecretKeySpec(secret.toByteArray(), HMAC_ALGORITHM))
            }
            .doFinal(input.toByteArray())
            .toBase64Url()

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val TOKEN_TTL_SECONDS = 86_400L
        private const val IAT_CLOCK_SKEW_SECONDS = 600L
        private const val OUTBOUND_DIRECTION = "outbound"

        /**
         * True if [signalingToken] carries the outbound external-call claims produced by
         * [makeOutboundCallToken]. Such a session needs an explicit `engine.initiateCall()`
         * after joining the channel; tokens of incoming (push-delivered) calls don't.
         */
        fun isOutboundCallToken(signalingToken: String): Boolean = runCatching {
            val payload = signalingToken.split('.')[1]
            JSONObject(String(Base64.getUrlDecoder().decode(payload)))
                .optJSONObject("externalCall")
                ?.optString("direction") == OUTBOUND_DIRECTION
        }.getOrDefault(false)
    }
}