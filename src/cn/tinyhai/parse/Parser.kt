package cn.tinyhai.parse

interface Parser <R> {
    suspend fun parse(html: String): R
}