package top.sspirits.blog.extension

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpServerResponse

fun HttpServerResponse.sendError(statusCode: HttpResponseStatus) {
    setStatusCode(statusCode.code()).end()
}
