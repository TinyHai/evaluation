package cn.tinyhai.parse

import org.jsoup.Jsoup
import java.lang.RuntimeException

object EvaluationFormListParser : Parser<List<Map<String, String>>> {

    override suspend fun parse(html: String): List<Map<String, String>> {
        val document = Jsoup.parse(html)

        println("开始解析评估列表")
        println("页面标题：" + document.title())

        val trs = document.select("table#user tbody > tr")

        val resultList = ArrayList<Map<String, String>>()
        for (tr in trs) {
            val isEvaluated = tr.child(3).text().trim() == "是"
            resultList += if (isEvaluated) {
                emptyMap()
            } else {
                parseToMap(tr.child(4).child(0).attr("name"))
            }
        }

        if (resultList.isEmpty()) {
            throw RuntimeException("没有可评教的项")
        }

        return resultList
    }

    private fun parseToMap(value: String): Map<String, String> {
        val values = value.split("#@")
        val resultMap = HashMap<String, String>()
        resultMap["wjbm"] = values[0]
        resultMap["bpr"] = values[1]
        resultMap["bprm"] = values[2]
        resultMap["wjmc"] = values[3]
        resultMap["pgnrm"] = values[4]
        resultMap["pgnr"] = values[5]
        resultMap["wjbz"] = "null"
        resultMap["oper"] = "wjShow"
        resultMap["pageSize"] = "300"
        resultMap["page"] = "1"
        resultMap["currentPage"] = "1"
        resultMap["pageNo"] = ""
        return resultMap
    }
}