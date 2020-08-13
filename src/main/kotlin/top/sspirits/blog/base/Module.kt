package top.sspirits.blog.base

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import org.koin.dsl.module

val appModule = module {
    single { (vertx: Vertx) -> Router.router(vertx) }
}
