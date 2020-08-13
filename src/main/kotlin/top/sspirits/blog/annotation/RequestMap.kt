package top.sspirits.blog.annotation

import top.sspirits.blog.base.HttpMethod

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestMap(val path: String = "/", val method: HttpMethod = HttpMethod.GET)
