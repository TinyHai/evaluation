package cn.tinyhai

import cn.tinyhai.evaluation.USER_AGENT_BASE
import kotlin.random.Random

object UserAgent {
    val random: String
        get() = USER_AGENT_BASE + Random.nextInt(1000) + "." + Random.nextInt(100)
}