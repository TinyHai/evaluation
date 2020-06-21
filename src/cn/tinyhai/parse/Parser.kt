package cn.tinyhai.parse

interface Parser <T> {
    suspend fun parse(html: String): T
}