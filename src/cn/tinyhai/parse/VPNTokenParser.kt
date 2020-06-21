package cn.tinyhai.parse

import org.jsoup.Jsoup

object VPNTokenParser : Parser<Map<String, String>> {

    override suspend fun parse(html: String): Map<String, String> {
        val document = Jsoup.parse(html)

        println("开始解析VPN Token")
        println("页面标题：" + document.title())

        val head = document.head()
        val tokenMeta = head.select("meta[name=csrf-token]")
        val token = tokenMeta.attr("content")

        return mapOf("authenticity_token" to token)
    }
}