package top.sspirits.blog.util

import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import top.sspirits.blog.annotation.Service
import top.sspirits.blog.base.CustomVerticle
import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

class ClassUtils {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ClassUtils::class.java)

        private fun findAndAddClassesInPackageByFile(
            packageName: String, packagePath: String, recursive: Boolean,
            classes: MutableList<KClass<*>>
        ) {
            val dir = File(packagePath)
            if (!dir.exists() || !dir.isDirectory) {
                logger.warn("there are no classes under $packageName")
                return
            }
            val files = dir.listFiles { file ->
                recursive && file.isDirectory || file.name.endsWith(".class")
            } ?: return
            for (file in files) {
                if (file.isDirectory) {
                    findAndAddClassesInPackageByFile(
                        packageName + "." + file.name,
                        file.absolutePath,
                        recursive,
                        classes
                    )
                } else {
                    val className = file.name.substring(0, file.name.length - 6)
                    classes.add(Class.forName("$packageName.$className").kotlin)
                }
            }
        }

        @Suppress("NAME_SHADOWING")
        fun getClasses(packageName: String, recursive: Boolean = true): List<KClass<*>> {
            var packageName = packageName
            val classes = ArrayList<KClass<*>>()
            val packageDirName = packageName.replace('.', '/')
            val dirs: Enumeration<URL>
            try {
                dirs = Thread.currentThread().contextClassLoader.getResources(packageDirName)
                while (dirs.hasMoreElements()) {
                    val url: URL = dirs.nextElement()
                    val protocol: String = url.protocol
                    if ("file" == protocol) {
                        val filePath = URLDecoder.decode(url.file, "UTF-8")
                        findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes)
                    } else if ("jar" == protocol) {
                        // TODO 这个分支待验证
                        // 如果是jar包文件
                        // 定义一个JarFile
                        var jar: JarFile
                        try {
                            // 获取jar
                            jar = (url.openConnection() as JarURLConnection).jarFile
                            // 从此jar包 得到一个枚举类
                            val entries: Enumeration<JarEntry> = jar.entries()
                            // 同样的进行循环迭代
                            while (entries.hasMoreElements()) {
                                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                                val entry: JarEntry = entries.nextElement()
                                var name: String = entry.name
                                // 如果是以/开头的
                                if (name[0] == '/') {
                                    // 获取后面的字符串
                                    name = name.substring(1)
                                }
                                // 如果前半部分和定义的包名相同
                                if (name.startsWith(packageDirName)) {
                                    val idx = name.lastIndexOf('/')
                                    // 如果以"/"结尾 是一个包
                                    if (idx != -1) {
                                        // 获取包名 把"/"替换成"."
                                        packageName = name.substring(0, idx).replace('/', '.')
                                    }
                                    // 如果可以迭代下去 并且是一个包
                                    if (idx != -1 || recursive) {
                                        // 如果是一个.class文件 而且不是目录
                                        if (name.endsWith(".class") && !entry.isDirectory) {
                                            // 去掉后面的".class" 获取真正的类名
                                            val className = name.substring(packageName.length + 1, name.length - 6)
                                            try {
                                                // 添加到classes
                                                classes.add(Class.forName("$packageName.$className").kotlin)
                                            } catch (e: ClassNotFoundException) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            logger.error("can not open ${url.path}", e)
                        }
                    }
                }
            } catch (e: IOException) {
                logger.error("can not open $packageDirName", e)
            }
            return classes
        }

        @Suppress("UNCHECKED_CAST")
        fun getServices(packageName: String): List<KClass<out CustomVerticle>> {
            val services = ArrayList<KClass<out CustomVerticle>>()
            val clsList = getClasses(packageName)
            for (cls in clsList) {
                try {
                    if (!cls.java.isAnonymousClass &&
                        !cls.java.isSynthetic &&
                        cls.isSubclassOf(CustomVerticle::class) &&
                        cls.findAnnotation<Service>() != null
                    ) {
                        services.add(cls as KClass<CustomVerticle>)
                    }
                } catch (e: UnsupportedOperationException) {
                    logger.debug(e)
                }
            }
            return services
        }
    }
}
