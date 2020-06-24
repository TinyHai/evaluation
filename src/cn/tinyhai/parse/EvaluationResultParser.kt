package cn.tinyhai.parse

import org.jsoup.Jsoup
import java.util.regex.Pattern

object EvaluationResultParser : Parser<Boolean> {
    override suspend fun parse(html: String): Boolean {
        val document = Jsoup.parse(html)

        println("开始解析评估结果")
        println("页面标题：" + document.title())

        val resultJs = document.body().select("script[language=JavaScript]").first().data()

        val regex = """alert\("(.*)"\);"""
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(resultJs)

        if (matcher.find()) {
            val resultMsg = matcher.group(1)
            return resultMsg.contains("失败").not()
        }

        println("未匹配到评估返回结果！")
        println("resultJs: \n$resultJs")

        return false
    }
}