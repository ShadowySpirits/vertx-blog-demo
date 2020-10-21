package top.sspirits.blog.util

import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ClassUtils {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ClassUtils::class.java)
        private val classMap = ConcurrentHashMap<String, List<KClass<*>>>()

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

        fun getClasses(packageName: String, recursive: Boolean = true): List<KClass<*>> {
            classMap[packageName]?.let {
                return it
            }
            var pkg = packageName
            val classes = ArrayList<KClass<*>>()
            val packageDirName = pkg.replace('.', '/')
            val dirs: Enumeration<URL>
            try {
                dirs = Thread.currentThread().contextClassLoader.getResources(packageDirName)
                while (dirs.hasMoreElements()) {
                    val url: URL = dirs.nextElement()
                    val protocol: String = url.protocol
                    if ("file" == protocol) {
                        val filePath = URLDecoder.decode(url.file, "UTF-8")
                        findAndAddClassesInPackageByFile(pkg, filePath, recursive, classes)
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
                                        pkg = name.substring(0, idx).replace('/', '.')
                                    }
                                    // 如果可以迭代下去 并且是一个包
                                    if (idx != -1 || recursive) {
                                        // 如果是一个.class文件 而且不是目录
                                        if (name.endsWith(".class") && !entry.isDirectory) {
                                            // 去掉后面的".class" 获取真正的类名
                                            val className = name.substring(pkg.length + 1, name.length - 6)
                                            try {
                                                // 添加到classes
                                                classes.add(Class.forName("$pkg.$className").kotlin)
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
            classMap[packageName] = classes
            return classes
        }

        fun findClassesWithAnnotation(
            packageName: String,
            targetAnnotation: KClass<out Annotation>
        ): List<KClass<*>> {
            val annotatedClasses = ArrayList<KClass<*>>()
            val classes = getClasses(packageName)
            for (cls in classes) {
                try {
                    if (!cls.java.isAnonymousClass &&
                        !cls.java.isSynthetic &&
                        cls.annotations.any { targetAnnotation.isInstance(it) }
                    ) {
                        annotatedClasses.add(cls)
                    }
                } catch (e: UnsupportedOperationException) {
                    logger.debug(e)
                }
            }
            return annotatedClasses
        }

        @Suppress("UNCHECKED_CAST")
        fun <C : Any> findTypedClassesWithAnnotation(
            packageName: String,
            targetAnnotation: KClass<out Annotation>,
            targetClass: KClass<C>
        ): List<KClass<C>> {
            val services = ArrayList<KClass<C>>()
            val classes = getClasses(packageName)
            for (cls in classes) {
                try {
                    if (!cls.java.isAnonymousClass &&
                        !cls.java.isSynthetic &&
                        cls.isSubclassOf(targetClass) &&
                        cls.annotations.any { targetAnnotation.isInstance(it) }
                    ) {
                        services.add(cls as KClass<C>)
                    }
                } catch (e: UnsupportedOperationException) {
                    logger.debug(e)
                }
            }
            return services
        }
    }
}
