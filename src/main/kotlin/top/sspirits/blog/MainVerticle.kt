package top.sspirits.blog

import io.vertx.core.http.CookieSameSite
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.undeployAwait
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import top.sspirits.blog.base.CustomVerticle
import top.sspirits.blog.base.appModule
import top.sspirits.blog.middleware.AccessLogHandler
import top.sspirits.blog.util.ClassUtils
import kotlin.reflect.KClass

class MainVerticle : CustomVerticle() {
    // Map<verticleClass, deploymentID>
    private val deploymentMap = HashMap<KClass<out CustomVerticle>, String>()
    private lateinit var httpServer: HttpServer

    override suspend fun start() {
        startKoin {
            slf4jLogger()
            modules(appModule)
        }
        // init root router and add global handler or middleware
        val router = getRootRouter().apply {
            route()
                .handler(AccessLogHandler())
                .handler(BodyHandler.create())
                .handler(SessionHandler.create(LocalSessionStore.create(vertx)).apply {
                    setSessionCookieName("blog-session")
                    setCookieHttpOnlyFlag(true)
                    setCookieSameSite(CookieSameSite.LAX)
                })
        }

        // deploy services
        deployServices(ClassUtils.getServices("top.sspirits.blog"))

        // start http server
        httpServer = vertx.createHttpServer()
            .requestHandler(router)
            .listenAwait(config.getInteger("server.port", 8080))
        logger.info("HTTP server started on port " + config.getInteger("server.port", 8080))
    }

    override suspend fun stop() {
        deploymentMap.forEach {
            vertx.undeployAwait(it.value)
            logger.info("Succeeded in undeploying ${it.key.java.simpleName}(${it.value})")
        }
        httpServer.closeAwait()
        logger.info("HTTP server shut down")
    }

    private suspend fun deployServices(verticleList: List<KClass<out CustomVerticle>>) {
        verticleList.forEach { clazz ->
            val id = vertx.deployVerticleAwait(clazz.java.canonicalName)
            deploymentMap[clazz] = id
            logger.info("Succeeded in deploying ${clazz.java.simpleName}(${id})")
        }
    }
}
