package cn.tinyhai.parse

import io.ktor.http.Parameters
import org.jsoup.Jsoup

object QuestionParametersParser : Parser<Parameters> {
    override suspend fun parse(html: String): Parameters {
        val document = Jsoup.parse(html)

        println("开始解析评估问卷")
        println("页面标题：" + document.title())

        val form = document.select("form[name=StDaForm]").first()
        val inputs = form.select("table[bgcolor=#A9C8F1] input[value~=^[0-9]+_1$]")
        inputs.addAll(form.select("> input[type=hidden]"))

        return Parameters.build {
            inputs.forEach {
                append(it.attr("name"), it.`val`())
            }
            set("xumanyzg", buildString {
                append("zg")
                val textAreas = document.getElementsByTag("textarea")
                for (idx in 1 until textAreas.size) {
                    append(textAreas[idx])
                    append(",")
                }
                if (textAreas.size > 2) {
                    setLength(length - 1)
                }
            })
            append("zgpj", "")
        }
    }
}