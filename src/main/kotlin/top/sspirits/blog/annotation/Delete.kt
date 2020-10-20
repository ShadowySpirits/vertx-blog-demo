package top.sspirits.blog.annotation

import top.sspirits.blog.base.HttpMethod

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Path(method = HttpMethod.DELETE)
annotation class Delete(val path: String = "/")
