package top.sspirits.blog.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import top.sspirits.blog.annotation.Get
import top.sspirits.blog.annotation.Path
import top.sspirits.blog.annotation.Post
import top.sspirits.blog.annotation.Service
import top.sspirits.blog.base.CustomVerticle
import top.sspirits.blog.base.HttpMethod
import top.sspirits.blog.extension.sendError

@Service("/schema")
class RestVerticle : CustomVerticle() {

    @Path(HttpMethod.GET, "/all")
    fun getAllSchemas(ctx: RoutingContext) {
        val allSchemasJson = json {
            obj(

            )
        }
        ctx.response()
            .putHeader("content-type", "application/json")
            .end(allSchemasJson.encode())
    }

    @Get("/:id")
    fun getSchema(ctx: RoutingContext) {
        val schemaJson = JsonObject()
        ctx.addEndHandler {
            it.result()
        }

        ctx.response()
            .putHeader("content-type", "application/json")
            .end(schemaJson.encode())
    }

    @Post("/")
    fun postSchema(ctx: RoutingContext) {
        val schemaJson = JsonObject()
        schemaJson.let {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(it.encode())
        } ?: ctx.response().sendError(HttpResponseStatus.BAD_REQUEST)
    }
}
