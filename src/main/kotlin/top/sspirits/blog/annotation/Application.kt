package top.sspirits.blog.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Application(val value: Array<String>)
