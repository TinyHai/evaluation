package cn.tinyhai.evaluation

import cn.tinyhai.exception.AlreadyEvaluatingException
import cn.tinyhai.parse.EvaluationFormListParser
import cn.tinyhai.parse.EvaluationResultParser
import cn.tinyhai.parse.QuestionFormParser
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.toByteArray
import kotlinx.coroutines.*
import java.lang.RuntimeException
import java.nio.charset.Charset

@KtorExperimentalAPI
object Evaluation {

    @Throws(AlreadyEvaluatingException::class)
    suspend fun startEvaluation(username: String, password: String) = coroutineScope {
        val helper = EvaluationHelper.obtain(username, password)

        val client = helper.login()

        val evaluationListPage = getEvaluationListPage(client)
        val evaluationFormList = EvaluationFormListParser.parse(evaluationListPage)

        val questionFormList = getAllQuestionForm(evaluationFormList, client)

        val evaluationList = questionFormList.map { formMap ->
            async {
                if (formMap.isEmpty()) {
                    true
                } else {
                    val parameters = Parameters.build {
                        formMap.forEach {
                            append(it.key, it.value)
                        }
                    }
                    val evaluateResponse = client.submitForm<HttpResponse>(parameters) {
                        url(URL_EDUCATIONAL_EVALUATE)
                    }
                    val html = evaluateResponse.charset()?.let { evaluateResponse.string(it) } ?: evaluateResponse.string()
                    if (evaluateResponse.status == HttpStatusCode.OK) {
                        EvaluationResultParser.parse(html)
                    } else {
                        println(evaluateResponse.status)
                        println(html)
                        false
                    }
                }
            }
        }

        val evaluationResult = evaluationList.map {
            try {
                it.await()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                false
            }
        }

        helper.dispose()

        evaluationResult
    }

    private suspend fun getAllQuestionForm(evaluationFormList: List<Map<String, String>>, client: HttpClient): List<Map<String, String>> {
        val questionFormList = ArrayList<Map<String, String>>(evaluationFormList.size)
        evaluationFormList.forEach { formMap ->
            questionFormList += if (formMap.isEmpty()) {
                formMap
            } else {
                val parameters = Parameters.build {
                    formMap.forEach {
                        append(it.key, it.value)
                    }
                }

                val response = client.submitForm<HttpResponse>(parameters) {
                    url(URL_EDUCATIONAL_QUESTION)
                }
                val html = response.charset()?.let { response.string(it) } ?: response.string()
                QuestionFormParser.parse(html)
            }
        }
        return questionFormList
    }

    private suspend fun getEvaluationListPage(client: HttpClient): String {
        val response = client.get<HttpResponse>(URL_EDUCATIONAL_LIST) {}
        when (response.status) {
            HttpStatusCode.OK -> {
                return response.charset()?.let { response.string(it) } ?: response.string()
            }
            else -> {
                println(response.request.headers[HttpHeaders.Cookie])
                throw RuntimeException("评估列表打开失败")
            }
        }
    }

    private suspend inline fun HttpResponse.string(charset: Charset = Charsets.UTF_8): String = content.toByteArray().toString(charset)
}