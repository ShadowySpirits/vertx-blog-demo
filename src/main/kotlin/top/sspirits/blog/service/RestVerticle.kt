package top.sspirits.blog.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ext.scope
import top.sspirits.blog.base.CustomVerticle
import top.sspirits.blog.base.HttpMethod
import top.sspirits.blog.annotation.RequestMap
import top.sspirits.blog.extension.sendError

@RequestMap(path = "/schema")
class RestVerticle : CustomVerticle() {

    @RequestMap(path = "/all")
    fun getAllSchemas(ctx: RoutingContext) {
        val allSchemasJson = json {
            obj(

            )
        }
        ctx.response()
            .putHeader("content-type", "application/json")
            .end(allSchemasJson.encode())
    }

    @RequestMap(path = "/:id")
    fun getSchema(ctx: RoutingContext) {
        val schemaJson = JsonObject()
        ctx.addEndHandler {
            it.result()
        }

        ctx.response()
            .putHeader("content-type", "application/json")
            .end(schemaJson.encode())
    }

    @RequestMap(path = "/", method = HttpMethod.POST)
    fun postSchema(ctx: RoutingContext) {
        val schemaJson = JsonObject()
        schemaJson.let {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(it.encode())
        } ?: ctx.response().sendError(HttpResponseStatus.BAD_REQUEST)
    }
}
