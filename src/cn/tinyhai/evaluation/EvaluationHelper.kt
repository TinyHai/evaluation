package cn.tinyhai.evaluation

import cn.tinyhai.aes.AESUtils
import cn.tinyhai.parse.EduFormParser
import cn.tinyhai.parse.VPNTokenParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.UserAgent
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.toByteArray
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.charset.Charset

@KtorExperimentalAPI
class EvaluationHelper private constructor() {

    private val cookieStore = HashSet<Cookie>()

    private var loginClientUsed = false

    private var evaluationClientUsed = false

    private val loginClient by lazy {
        loginClientUsed = true
        generateLoginClient()
    }

    val evaluationClient by lazy {
        evaluationClientUsed = true
        generateEvaluationClient(cookieStore)
    }

    fun dispose() {
        if (loginClientUsed) {
            try {
                loginClient.close()
            } catch (e: Exception) {}
        }
        if (evaluationClientUsed) {
            try {
                evaluationClient.close()
            } catch (e: Exception) {}
        }
    }

    fun getCookieStoreString(): String {
        return cookieStore.map { "$it\n" }.toString()
    }

    companion object {

        private fun generateLoginClient() =
            HttpClient(CIO) {
                followRedirects = false

                install(UserAgent) {
                    agent = cn.tinyhai.UserAgent.random
                }

                install(HttpCookies) {
                    storage = AcceptAllCookiesStorage()
                }
            }

        private fun generateEvaluationClient(cookies: Collection<Cookie>) =
            HttpClient(CIO) {
                followRedirects = false

                install(UserAgent) {
                    agent = cn.tinyhai.UserAgent.random
                }

                install(HttpCookies) {
                    storage = ConstantCookiesStorage(*cookies.map { it.copy(domain = ".ecit.cn", path = "/") }.toTypedArray())
                }
            }

        private fun generateVPNLoginParameters(
            username: String,
            password: String,
            tokenMap: Map<String, String>
        ) = Parameters.build {
                tokenMap.forEach {
                    append(it.key, it.value)
                }
                append("utf8", "✓")
                append("user[login]", username)
                append("user[password]", password)
                append("user[dymatice_code]", "unknown")
                append("commit", "登录 Login")
            }

        suspend fun obtain(username: String, password: String): EvaluationHelper {
            val helper = EvaluationHelper()
            val client = helper.loginClient

            val vpnResponse = okOrThrow("VPN登陆页面打开失败") {
                client.get(URL_VPN_LOGIN)
            }

            val tokenMap = VPNTokenParser.parse(vpnResponse.content.toByteArray().toString(Charsets.UTF_8))

            val vpnLoginParameters = generateVPNLoginParameters(username, password, tokenMap)
            val vpnLoginResponse = foundOrThrow("VPN登陆失败") {
                client.submitForm(vpnLoginParameters) {
                    url(URL_VPN_LOGIN)
                }
            }

            val location = vpnLoginResponse.headers["Location"]!!
            val vpnKeyResponse = foundOrThrow("VPN Key获取失败") {
                client.get(location)
            }

            helper.cookieStore.addAll(vpnKeyResponse.setCookie().filter { it.domain == ".ecit.cn" })

            val eduResponse = okOrThrow("教务系统页面打开失败") {
                client.get(URL_EDUCATIONAL_LOGIN)
            }

            val formMap = EduFormParser.parse(eduResponse.string())
            val key = formMap.remove("key")!!
            val eduLoginParameters = Parameters.build {
                append("username", username)
                append("password", AESUtils.encrypt(password, key))
                formMap.forEach {
                    append(it.key, it.value)
                }
            }
            val eduLoginResponse = foundOrThrow("教务系统登陆失败") {
                client.submitForm(eduLoginParameters) {
                    url(URL_EDUCATIONAL_LOGIN)
                }
            }

            var foundResponse = eduLoginResponse

            foundResponse = client.get(foundResponse.headers["Location"]!!)
            helper.cookieStore.addAll(foundResponse.setCookie())

            /**
            * 跳转到 Constants.URL_EDUCATIONAL_HOME 为止
            * */
            while (foundResponse.status == HttpStatusCode.Found) {
                foundResponse = client.get(foundResponse.headers["Location"]!!)
            }

            helper.cookieStore.addAll(eduLoginResponse.setCookie())

//            println(helper.getCookieStoreString())

            return helper
        }

        private inline fun okOrThrow(errorMsg: String, block: () -> HttpResponse): HttpResponse {
            val response = block()
            if (response.status == HttpStatusCode.OK) {
                return response
            }
            throw RuntimeException(errorMsg)
        }

        private inline fun foundOrThrow(errorMsg: String, block: () -> HttpResponse): HttpResponse {
            val response = block()
            if (response.status == HttpStatusCode.Found) {
                return response
            }
            throw RuntimeException(errorMsg)
        }

        private suspend inline fun HttpResponse.string(charset: Charset = Charsets.UTF_8): String = content.toByteArray().toString(charset)
    }
}