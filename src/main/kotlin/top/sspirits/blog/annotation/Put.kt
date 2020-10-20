package top.sspirits.blog.annotation

import top.sspirits.blog.base.HttpMethod

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Path(method = HttpMethod.PUT)
annotation class Put(val path: String = "/")
