package cn.tinyhai.parse

import org.jsoup.Jsoup

object QuestionFormParser : Parser<Map<String, String>> {
    override suspend fun parse(html: String): Map<String, String> {
        val document = Jsoup.parse(html)

        println("开始解析评估问卷")
        println("页面标题：" + document.title())

        val form = document.select("form[name=StDaForm]").first()
        val inputs = form.select("table[bgcolor=#A9C8F1] input[value~=[0-9]+_1]")
        inputs.addAll(form.select("> input[type=hidden]"))

        val resultMap = HashMap<String, String>()
        inputs.forEach {
            resultMap[it.attr("name")] = it.`val`()
        }

        resultMap["xumanyzg"] = "zg"
        resultMap["wjbz"] = ""
        resultMap["zgpj"] = ""

        return resultMap
    }
}