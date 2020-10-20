package top.sspirits.blog.util

import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass


class AnnotationUtils {
    companion object {
        fun <A : Annotation> findAnnotation(annotatedElement: KAnnotatedElement, annotationType: KClass<A>): A? {
            return findAnnotation(annotatedElement, annotationType, HashSet())
        }

        @Suppress("UNCHECKED_CAST")
        private fun <A : Annotation> findAnnotation(
            annotatedElement: KAnnotatedElement, annotationType: KClass<A>, visited: MutableSet<Annotation>
        ): A? {
            var annotation = annotatedElement.annotations.firstOrNull { annotationType.isInstance(it) }
            if (annotation != null) {
                return annotation as A
            }
            for (declaredAnn in annotatedElement.annotations) {
                val declaredType = declaredAnn.annotationClass
                if (!isBuildInAnnotation(declaredType) && visited.add(declaredAnn)) {
                    annotation = findAnnotation(declaredType as KAnnotatedElement, annotationType, visited)
                    if (annotation != null) {
                        return annotation
                    }
                }
            }
            return null
        }

        private fun isBuildInAnnotation(annotation: KClass<out Annotation>): Boolean {
            val packageName = annotation.java.packageName
            return packageName.startsWith("kotlin") || packageName.startsWith("java") || packageName.startsWith("sun")
        }
    }
}
