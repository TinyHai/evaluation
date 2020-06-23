package cn.tinyhai.parse

import io.ktor.http.Parameters
import org.jsoup.Jsoup
import java.lang.RuntimeException

object EvaluationParametersListParser : Parser<List<Parameters>> {

    override suspend fun parse(html: String): List<Parameters> {
        val document = Jsoup.parse(html)

        println("开始解析评估列表")
        println("页面标题：" + document.title())

        val trs = document.select("table#user tbody > tr")

        val resultList = ArrayList<Parameters>()
        for (tr in trs) {
            val isEvaluated = tr.child(3).text().trim() == "是"
            resultList += if (isEvaluated) {
//                parseToParameters(tr.child(4).child(0).attr("name"))
                Parameters.Empty
            } else {
                parseToParameters(tr.child(4).child(0).attr("name"))
            }
        }

        if (resultList.isEmpty()) {
            throw RuntimeException("没有可评教的项")
        }

        return resultList
    }

    private fun parseToParameters(value: String): Parameters {
        val values = value.split("#@")
        return Parameters.build {
            append("wjbm", values[0])
            append("bpr", values[1])
            append("bprm", values[2])
            append("wjmc", values[3])
            append("pgnrm", values[4])
            append("pgnr", values[5])
            append("wjbz", "null")
            append("oper", "wjShow")
            append("pageSize", "300")
            append("page", "1")
            append("currentPage", "1")
            append("pageNo", "")
        }
    }
}