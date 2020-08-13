package top.sspirits.blog.middleware

import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.HttpVersion
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext

class AccessLogHandler : Handler<RoutingContext> {
    private val logger: Logger = LoggerFactory.getLogger("[AccessLog]")
    private val slowLogger: Logger = LoggerFactory.getLogger("[SlowLog]")

    private val logSlowThreshold: Long = 300

    override fun handle(ctx: RoutingContext) {
        val startTimeMillis = System.currentTimeMillis()

        ctx.addBodyEndHandler { log(ctx, startTimeMillis) }
        ctx.next()
    }

    private fun log(ctx: RoutingContext, startTimeMillis: Long) {
        val duration = System.currentTimeMillis() - startTimeMillis
        val request = ctx.request()
        val response = ctx.response()

        val message = logMessage(request, response, duration)
        when (ctx.response().statusCode) {
            in 500 until Int.MAX_VALUE -> logger.error(message)
            in 400 until 500 -> logger.warn(message)
            else -> logger.info(message)
        }
        if (duration > logSlowThreshold) {
            // method uri version status content-length duration
            slowLogger.warn("${request.method()} ${request.uri()} ${response.statusCode} ${response.bytesWritten()} $duration")
        }
    }

    /**
     * remote-client - - "method uri version" status content-length "referrer" "user-agent" duration
     */
    private fun logMessage(request: HttpServerRequest, response: HttpServerResponse, duration: Long): String {
        var remoteClient: String? = request.remoteAddress()?.host()
        val versionFormatted = when (request.version()) {
            HttpVersion.HTTP_1_0 -> "HTTP/1.0"
            HttpVersion.HTTP_1_1 -> "HTTP/1.1"
            HttpVersion.HTTP_2 -> "HTTP/2.0"
            else -> "-"
        }
        // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
        val headers = request.headers()
        var referrer: String? =
            if (headers.contains("referrer")) headers.get("referrer") else headers.get("referer")
        var userAgent: String? = request.headers().get("user-agent")
        remoteClient = remoteClient ?: "-"
        referrer = referrer ?: "-"
        userAgent = userAgent ?: "-"

        return String.format(
            "%s - - \"%s %s %s\" %d %d \"%s\" \"%s\" %d",
            remoteClient,
            request.method(),
            request.uri(),
            versionFormatted,
            response.statusCode,
            response.bytesWritten(),
            referrer,
            userAgent,
            duration
        )
    }
}
