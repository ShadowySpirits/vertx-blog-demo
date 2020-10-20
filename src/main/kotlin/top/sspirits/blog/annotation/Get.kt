package top.sspirits.blog.annotation

import top.sspirits.blog.base.HttpMethod

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Path(method = HttpMethod.GET)
annotation class Get(val path: String = "/")
