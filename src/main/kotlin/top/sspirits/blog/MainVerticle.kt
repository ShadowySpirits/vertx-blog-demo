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
import org.koin.core.get
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import top.sspirits.blog.annotation.Application
import top.sspirits.blog.annotation.Component
import top.sspirits.blog.annotation.Service
import top.sspirits.blog.base.ComponentMode
import top.sspirits.blog.base.CustomVerticle
import top.sspirits.blog.base.frameworkModule
import top.sspirits.blog.middleware.AccessLogHandler
import top.sspirits.blog.util.ClassUtils
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.system.exitProcess

open class MainVerticle : CustomVerticle() {
    // Map<verticleClass, deploymentID>
    private val deploymentMap = HashMap<KClass<out CustomVerticle>, String>()
    private lateinit var httpServer: HttpServer

    override suspend fun start() {
        val application = this::class.findAnnotation<Application>()
        if (application == null) {
            logger.error("no application annotation found")
            exitProcess(1)
        }
        val packageNames = if (application.value.isEmpty()) arrayOf(this::class.java.packageName) else application.value

        // init ioc
        startKoin {
            slf4jLogger()
            val appModule = module {}
            packageNames.forEach { packageName ->
                ClassUtils.findClassesWithAnnotation(packageName, Component::class)
                    .forEach { beanClass ->
                        loadBean(appModule, beanClass, beanClass.findAnnotation()!!)
                    }
            }
            modules(frameworkModule, appModule)
        }

        // init root router and add global handler or middleware
        val router = getRootRouter().apply {
            route()
                .handler(get<AccessLogHandler>())
                .handler(BodyHandler.create())
                .handler(SessionHandler.create(LocalSessionStore.create(vertx)).apply {
                    setSessionCookieName("blog-session")
                    setCookieHttpOnlyFlag(true)
                    setCookieSameSite(CookieSameSite.LAX)
                })
        }

        // deploy services
        val services = ArrayList<KClass<CustomVerticle>>()
        packageNames.forEach { packageName ->
            services.addAll(
                ClassUtils.findTypedClassesWithAnnotation(
                    packageName,
                    Service::class,
                    CustomVerticle::class
                )
            )
        }
        deployServices(services)

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

    private fun <T : Any> Scope.newInstance(parameter: Array<KClass<*>>, cls: KClass<T>): T {
        return if (parameter.isEmpty()) {
            cls.java.getDeclaredConstructor().newInstance()
        } else {
            cls.java.getDeclaredConstructor()
                .newInstance(*parameter.mapTo(ArrayList<Any>()) { get(it) }
                    .toArray())
        }
    }

    private fun loadBean(appModule: Module, beanClass: KClass<*>, beanDef: Component) {
        if (beanDef.name.isBlank()) {
            if (beanDef.mode == ComponentMode.SINGLE) {
                appModule.apply {
                    single { newInstance(beanDef.parameter, beanClass) }.bind(beanClass)
                }
            } else {
                appModule.apply {
                    factory { newInstance(beanDef.parameter, beanClass) }.bind(beanClass)
                }
            }
        } else {
            if (beanDef.mode == ComponentMode.SINGLE) {
                appModule.apply {
                    single(named(beanDef.name)) { newInstance(beanDef.parameter, beanClass) }.bind(beanClass)
                }
            } else {
                appModule.apply {
                    factory(named(beanDef.name)) { newInstance(beanDef.parameter, beanClass) }.bind(beanClass)
                }
            }
        }
    }

    private suspend fun deployServices(verticleList: List<KClass<out CustomVerticle>>) {
        verticleList.forEach { clazz ->
            val id = vertx.deployVerticleAwait(clazz.java.canonicalName)
            deploymentMap[clazz] = id
            logger.info("Succeeded in deploying ${clazz.java.simpleName}(${id})")
        }
    }
}
