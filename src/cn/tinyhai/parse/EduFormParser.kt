package cn.tinyhai.parse

import org.jsoup.Jsoup
import java.lang.RuntimeException
import java.util.regex.Pattern

object EduFormParser : Parser<MutableMap<String, String>> {
    override suspend fun parse(html: String): MutableMap<String, String> {
        val document = Jsoup.parse(html)

        println("开始解析教务登陆")
        println("页面标题：" + document.title())

        val head = document.head()
        val js = head.children().last().data()
        val key = parseKey(js)

        val body = document.body()
        val casLoginForm = body.select("form#casLoginForm")
        val inputs = casLoginForm.select("> input")
        return mutableMapOf(*inputs.toList().map { it.attr("name") to it.`val`() }.toTypedArray(), "key" to key)
    }

    private fun parseKey(js: String): String {
        val regex = "\"([a-zA-Z0-9]{16})\""
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(js)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            throw RuntimeException("AES key not found! js: \n$js")
        }
    }
}