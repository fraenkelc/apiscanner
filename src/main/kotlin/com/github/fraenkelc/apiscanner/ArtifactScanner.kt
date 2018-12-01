/*
 * Copyright 2018 Christian Fraenkel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.fraenkelc.apiscanner

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.streams.toList

class ArtifactScanner(private val artifactName: String, private val artifactFiles: Collection<File>) {
    fun scanArtifact(): ArtifactScanResult {
        val declared = mutableSetOf<String>()
        val consumed = mutableSetOf<String>()
        artifactFiles.forEach { artifact ->
            if (artifact.exists()) {
                val nodes = when {
                    artifact.isDirectory -> Files.find(artifact.toPath(), Integer.MAX_VALUE, { path, _ ->
                        path.toFile().extension == "class"
                    }).map {
                        Files.newInputStream(it).use { stream ->
                            ClassNode().also { node ->
                                ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG)
                            }
                        }
                    }.toList()
                    artifact.extension == "jar" -> JarFile(artifact).use { jarFile ->
                        jarFile.stream()
                                .filter { !it.isDirectory }
                                .filter { it.name.endsWith(".class") }
                                .map {
                                    ClassNode().also { node ->
                                        jarFile.getInputStream(it).use { stream ->
                                            ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG)
                                        }
                                    }
                                }.toList()
                    }
                    else -> listOf()
                }
                nodes.filterNotNull().forEach { node ->
                    handleClassData(node, declared, consumed)
                }
            }
        }
        return ArtifactScanResult(artifactName, declared, consumed)
    }

    private fun handleClassData(node: ClassNode, declared: MutableSet<String>, consumed: MutableSet<String>) {
        declared += node.name.replace('/', '.')
        node.superName?.apply { consumed += this.replace('/', '.') }
        consumed += node.interfaces?.map { it.replace('/', '.') } ?: listOf()
        node.visibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.invisibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.visibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }
        node.invisibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }

        node.fields.filterNotNull().filter { it.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) > 0 }
                .forEach { handleFieldData(it, declared, consumed) }
        node.methods.filterNotNull().filter { it.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) > 0 }
                .forEach { handleMethodData(it, declared, consumed) }
    }

    private fun handleMethodData(node: MethodNode, declared: MutableSet<String>, consumed: MutableSet<String>) {
        // It should be enough to track that we provide the class, if we in turn only track class consumption.
        // As such we do not need to track the methods we provide.
        consumed += node.exceptions.map { it.replace('/', '.') }
        val type = Type.getType(node.desc)
        objectType(type) { objectType(it.returnType) { ri -> ri.className } }?.let { consumed += it }
        type.argumentTypes?.apply { consumed += mapNotNull { tp -> objectType(tp) { it.className } } }
        node.visibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.invisibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.visibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }
        node.invisibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }
        node.visibleParameterAnnotations?.apply { filterNotNull().flatMap { it.asIterable() }.filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.invisibleParameterAnnotations?.apply { filterNotNull().flatMap { it.asIterable() }.filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
    }

    private fun handleFieldData(node: FieldNode, declared: MutableSet<String>, consumed: MutableSet<String>) {
        // It should be enough to track that we provide the class, if we in turn only track class consumption.
        // As such we do not need to track the fields we provide.
        objectType(getType(node.desc)) { it.className }?.let { consumed += it }
        node.visibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.invisibleAnnotations?.apply { filterNotNull().forEach { handleAnnotationData(it, declared, consumed) } }
        node.visibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }
        node.invisibleTypeAnnotations?.apply { filterNotNull().forEach { handleTypeAnnotationData(it, declared, consumed) } }
    }

    private tailrec fun objectType(type: Type, typeExtractor: (Type) -> String?): String? =
            when {
                type.sort == OBJECT -> typeExtractor(type)
                type.sort == ARRAY -> objectType(type.elementType, typeExtractor)
                else -> null
            }


    private fun handleTypeAnnotationData(node: TypeAnnotationNode, declared: MutableSet<String>, consumed: MutableSet<String>) {
        handleAnnotationData(node, declared, consumed)
    }

    private fun handleAnnotationData(node: AnnotationNode, @Suppress("UNUSED_PARAMETER") declared: MutableSet<String>, consumed: MutableSet<String>) {
        consumed += Type.getType(node.desc).className

        val pairs = mutableMapOf<String, Any>()
        node.values?.let {
            val iterator = it.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                val value = iterator.next()
                pairs[key] = value
            }
        }
        pairs.values.forEach { handleAnnotationValue(it, declared, consumed) }
    }

    /**
     * Deal with the Annotation value data.
     *
     * See [org.objectweb.asm.tree.AnnotationNode.values] for details on the possible contents.
     */
    private fun handleAnnotationValue(value: Any, declared: MutableSet<String>, consumed: MutableSet<String>) {
        /**
         * The name value pairs of this annotation. Each name value pair is stored as two consecutive
         * elements in the list. The name is a {@link String}, and the value may be a {@link Byte}, {@link
         * Boolean}, {@link Character}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
         * {@link Double}, {@link String} or {@link org.objectweb.asm.Type}, or a two elements String
         * array (for enumeration values), an {@link AnnotationNode}, or a {@link List} of values of one
         * of the preceding types. The list may be {@literal null} if there is no name value pair.
         */
        when (value) {
            is List<*> -> value.forEach { handleAnnotationValue(it!!, declared, consumed) }
            is Type -> consumed += value.className
            is Array<*> -> consumed += Type.getType(value[0] as String).className
            is AnnotationNode -> handleAnnotationData(value, declared, consumed)
        }
    }
}

data class ArtifactScanResult(val artifactName: String,
                              val declared: Set<String> = setOf(),
                              val consumed: Set<String> = setOf())

