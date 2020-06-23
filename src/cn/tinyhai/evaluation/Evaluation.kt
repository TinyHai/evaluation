package cn.tinyhai.evaluation

import cn.tinyhai.exception.AlreadyEvaluatingException
import cn.tinyhai.parse.EvaluationParametersListParser
import cn.tinyhai.parse.EvaluationResultParser
import cn.tinyhai.parse.QuestionParametersParser
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
        val evaluationParametersList = EvaluationParametersListParser.parse(evaluationListPage)

        val questionParametersList = getAllQuestionParameters(evaluationParametersList, client)

        val evaluationDeferredList = questionParametersList.map { parameters ->
            async {
                if (parameters.isEmpty()) {
                    true
                } else {
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

        val evaluationResult = evaluationDeferredList.map {
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

    private suspend fun getAllQuestionParameters(evaluationParametersList: List<Parameters>, client: HttpClient): List<Parameters> {
        val questionParametersList = ArrayList<Parameters>(evaluationParametersList.size)
        evaluationParametersList.forEach { parameters ->
            questionParametersList += if (parameters.isEmpty()) {
                parameters
            } else {
                val response = client.submitForm<HttpResponse>(parameters) {
                    url(URL_EDUCATIONAL_QUESTION)
                }
                val html = response.charset()?.let { response.string(it) } ?: response.string()
                QuestionParametersParser.parse(html)
            }
        }
        return questionParametersList
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