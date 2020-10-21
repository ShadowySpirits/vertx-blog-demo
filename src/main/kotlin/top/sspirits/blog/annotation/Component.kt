package top.sspirits.blog.annotation

import top.sspirits.blog.base.ComponentMode
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Component(
    val name: String = "",
    val mode: ComponentMode = ComponentMode.SINGLE,
    val parameter: Array<KClass<*>> = []
)
