package cn.tinyhai

import cn.tinyhai.aes.AESUtils
import cn.tinyhai.evaluation.EvaluationHelper
import cn.tinyhai.parse.EvaluationParametersListParser
import cn.tinyhai.parse.QuestionParametersParser
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.FileReader

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }

    @Test
    fun testVPNLogin() = runBlocking {
        EvaluationHelper.obtain("", "")
        Unit
    }

    @Test
    fun testQuestionParser() = runBlocking {
        val html = BufferedReader(FileReader("test/question.html")).use { it.readText() }
        println(QuestionParametersParser.parse(html))
        Unit
    }

    @Test
    fun testEvaluationFormListParser() = runBlocking {
        val html = BufferedReader(FileReader("test/list.html")).use { it.readText() }
        println(EvaluationParametersListParser.parse(html))
        Unit
    }

    @Test
    fun testAESUtils() {
        val str = "hello"
        val key = "a1b2c3d4e5f6g7h8"
        val encryptedString = AESUtils.encrypt(str, key)
        val decodeString = AESUtils.decrypt(encryptedString, key)
        println(encryptedString)
        println(decodeString)
    }
}
