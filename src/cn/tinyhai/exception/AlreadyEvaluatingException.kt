package cn.tinyhai.exception

class AlreadyEvaluatingException(username: String)
    : EvaluationException("${username}正在评教中")