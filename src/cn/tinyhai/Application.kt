package cn.tinyhai

import cn.tinyhai.evaluation.Evaluation
import cn.tinyhai.exception.AlreadyEvaluatingException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.content.*
import io.ktor.request.receiveParameters
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import kotlinx.html.*

fun main(args: Array<String>): Unit = io.ktor.server.tomcat.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    routing {
        static("js") {
            resources("js")
        }

        get("/") {
            call.respondRedirect("/evaluation")
            return@get
        }

        route("/evaluation") {
            get {
                val error = call.parameters["error"]
                println("evaluation访问")
                call.respondHtml {
                    head {
                        title("一键评教")
                        script { src = "https://cdn.bootcss.com/jquery/3.4.1/jquery.js" }
                        script(ScriptType.textJavaScript) {
                            src = "/js/evaluation.js"
                        }
                    }
                    body {
                        if (error != null) {
                            p {
                                style = "color: red;"
                                + error
                            }
                        }
                        form("/evaluation", FormEncType.applicationXWwwFormUrlEncoded, FormMethod.post) {
                            id = "evaluationForm"

                            + "学号: "
                            textInput {
                                name = "username"
                            }
                            br
                            + "密码: "
                            passwordInput{
                                name = "password"
                            }
                            br
                            submitInput {
                                onClick = "beginEvaluation()"
                                value = "开始评教"
                            }
                            resetInput {
                                value = "重置"
                            }
                        }
                    }
                }
            }
            post {
                val form = call.receiveParameters()
                val username = form["username"]
                val password = form["password"]
                if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                    call.respondText("请传入账号和密码")
                    return@post
                }

                var successCount = 0
                var failCount = 0

                try {
                    val all = withContext(coroutineContext) {
                        Evaluation.startEvaluation(username, password).also {
                            it.forEach { result ->
                                if (result) {
                                    successCount++
                                } else {
                                    failCount++
                                }
                            }
                        }.size
                    }
                    call.respondText("总共可评教数：$all\n已评教：$successCount\n评教失败：$failCount", ContentType.Text.Plain)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    call.respondText("评教出错：${e.message}")
                } catch (e: AlreadyEvaluatingException) {
                    e.printStackTrace()
                    call.respondText(e.message!!, ContentType.Text.Plain)
                }
            }
        }
    }
}

