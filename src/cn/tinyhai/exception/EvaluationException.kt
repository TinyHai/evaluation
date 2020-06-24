package cn.tinyhai.exception

import java.lang.RuntimeException

open class EvaluationException(val reason: String) : RuntimeException()