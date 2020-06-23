package cn.tinyhai.parse

import org.jsoup.Jsoup

object VPNTokenParser : Parser<Pair<String, String>> {

    override suspend fun parse(html: String): Pair<String, String> {
        val document = Jsoup.parse(html)

        println("开始解析VPN Token")
        println("页面标题：" + document.title())

        val head = document.head()
        val tokenMeta = head.select("meta[name=csrf-token]")
        val token = tokenMeta.attr("content")

        return "authenticity_token" to token
    }
}