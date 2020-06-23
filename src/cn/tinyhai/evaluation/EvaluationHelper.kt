package cn.tinyhai.evaluation

import cn.tinyhai.aes.AESUtils
import cn.tinyhai.exception.AlreadyEvaluatingException
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.charset.Charset
import kotlin.coroutines.coroutineContext

@KtorExperimentalAPI
class EvaluationHelper private constructor(
        private val username: String,
        private val password: String
) {
    private val cookieStore = HashSet<Cookie>()

    private val loginClient = lazy {
        generateLoginClient()
    }

    private val evaluationClient = lazy {
        generateEvaluationClient(cookieStore)
    }

    /**
     * @return 携带Cookies的 HttpClient
     * @throws RuntimeException 登陆出现任何异常都会抛出并携带异常原因
     **/
    suspend fun login(): HttpClient {
        val client = loginClient.value

        // 打开VPN页面获取随机Token
        val vpnResponse = okOrThrow("VPN登陆页面打开失败") {
            client.get(URL_VPN_LOGIN)
        }

        val tokenMap = VPNTokenParser.parse(vpnResponse.content.toByteArray().toString(Charsets.UTF_8))

        // 登陆VPN
        val vpnLoginParameters = generateVPNLoginParameters(username, password, tokenMap)
        val vpnLoginResponse = foundOrThrow("VPN登陆失败") {
            client.submitForm(vpnLoginParameters) {
                url(URL_VPN_LOGIN)
            }
        }

        // 获取必要的Cookies
        val location = vpnLoginResponse.headers["Location"]!!
        val vpnKeyResponse = foundOrThrow("VPN Key获取失败") {
            client.get(location)
        }

        cookieStore.addAll(vpnKeyResponse.setCookie().filter { it.domain == ".ecit.cn" })

        // 打开教务系统获取随机密钥
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

        // 登陆教务系统
        val eduLoginResponse = foundOrThrow("教务系统登陆失败") {
            client.submitForm(eduLoginParameters) {
                url(URL_EDUCATIONAL_LOGIN)
            }
        }

        var foundResponse = eduLoginResponse

        // 获取必要的Cookies
        foundResponse = client.get(foundResponse.headers["Location"]!!)
        cookieStore.addAll(foundResponse.setCookie())

        /*
        * 跳转到 Constants.URL_EDUCATIONAL_HOME 为止
        * 获取JSESSIONID Cookies
        * */
        while (foundResponse.status == HttpStatusCode.Found) {
            foundResponse = client.get(foundResponse.headers["Location"]!!)
        }

        cookieStore.addAll(eduLoginResponse.setCookie())

//      println(helper.getCookieStoreString())

        return evaluationClient.value
    }

    private suspend fun logout() {
        if (loginClient.isInitialized()) {
            val client = loginClient.value
            val response = client.get<HttpResponse>(URL_VPN_LOGOUT)
            val cookies = response.setCookie()
            if (cookies.isEmpty()) {
                throw RuntimeException("VPN登出失败")
            }
            println(cookies.map { it.name to it.value })
        } else {
            throw RuntimeException("VPN未登陆，不需要登出")
        }
    }

    suspend fun dispose() {
        logout()

        if (loginClient.isInitialized()) {
            try {
                loginClient.value.close()
            } catch (e: Exception) {}
        }
        if (evaluationClient.isInitialized()) {
            try {
                evaluationClient.value.close()
            } catch (e: Exception) {}
        }
        recordMutex.withLock {
            evaluationRecord.remove(username)
        }
    }

    fun getCookieStoreString(): String {
        return cookieStore.map { "$it\n" }.toString()
    }

    companion object {

        private val evaluationRecord = ArrayList<String>()

        private val waitJobMap = HashMap<String, ArrayList<Job>>()

        private val recordMutex = Mutex()

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

        suspend fun cancelWaitingRequest(username: String) {
            recordMutex.withLock {
                waitJobMap[username]?.forEach {
                    it.cancel()
                }
            }
        }

        suspend fun obtain(username: String, password: String): EvaluationHelper {
            recordMutex.withLock {
                if (evaluationRecord.contains(username)) {
                    throw AlreadyEvaluatingException(username)
                } else {
                    evaluationRecord.add(username)
                }
            }

            return EvaluationHelper(username, password)
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