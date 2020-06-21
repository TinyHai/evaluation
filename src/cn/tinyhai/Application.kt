package cn.tinyhai

import cn.tinyhai.evaluation.Evaluation
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.html.respondHtml
import io.ktor.request.receiveParameters
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import kotlinx.html.*
import java.lang.RuntimeException

fun main(args: Array<String>): Unit = io.ktor.server.tomcat.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

//    install(ContentNegotiation) {
//        jackson {
//            enable(SerializationFeature.INDENT_OUTPUT)
//        }
//    }

    routing {
        route("/evaluation") {
            get {
                val error = call.parameters["error"]
                call.respondHtml {
                    head {
                        title("一键评教")
                    }
                    body {
                        if (error != null) {
                            p {
                                style = "color: red;"
                                + error
                            }
                        }
                        form("/evaluation", FormEncType.applicationXWwwFormUrlEncoded, FormMethod.post) {
                            + "学号: "; input(InputType.text, name = "username") { required = true }
                            br
                            + "密码: "; input(InputType.password, name = "password") { required = true }
                            br
                            submitInput {
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
                    call.respondText("总共可评教数：$all\n已评教：$successCount\n评教失败：$failCount")
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    call.respondText("评教出错：${e.message}")
                }
            }
        }
    }
}

