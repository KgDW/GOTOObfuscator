package org.gotoobfuscator.transformer.transformers.rename

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.obj.ClassWrapper
import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.dictionary.IDictionary
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.FileOutputStream
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Method rename by radon
 */

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ClassRename : Transformer("ClassRename") {
    companion object {
        @JvmStatic
        val exclude = ArrayList<String>()
    }

    private val baseMethods = arrayOf("hashCode()I","equals(Ljava/lang/Object;)Z","clone()Ljava/lang/Object;","toString()Ljava/lang/String;","finalize()V")

    private val mapping = HashMap<String,String>()
    private val fieldMapping = HashMap<String,String>()
    private val methodMapping = HashMap<String,String>()

    private val dictionary = IDictionary.newDictionary()
    private val fieldDictionary = IDictionary.newDictionary()
    private val methodDictionary = IDictionary.newDictionary()

    private val classTreeMap = HashMap<String,ClassTree>()

    private fun transform() {
        val classWrappers = Obfuscator.Instance.classes.values
        val classes = LinkedList<ClassNode>()

        classWrappers.forEach { classes.add(it.classNode) }

        classes.sortBy {
            -(it.name.length - it.name.replace("$","").length)
        }

        print("Remapping classes")

        MainForEach@ for (node in classes) {
            if (isExclude(node)) continue@MainForEach

            for (method in node.methods) {
                if (Modifier.isNative(method.access)) {
                    continue@MainForEach
                }
            }

            remap(node)
        }

        print("Building tree")

        for (node in classes) {
            buildTree(node,null)
        }

        print("Remapping field and method")

        MainForEach@ for (node in classes) {
            if (isExclude(node)) continue@MainForEach

            for (field in node.fields) {
                fieldMapping["${node.name}.${field.name}${field.desc}"] = fieldDictionary.get()
            }

            for (method in node.methods) {
                method.localVariables?.forEach {
                    if (it.name != "this")
                        it.name = "var${it.index}"
                }

                if (invalidMethodName(method.name,method.desc))
                    continue

                if (Modifier.isNative(method.access)) continue

                if (canRemapMethod(HashSet(),node,node,method)) {
                    remapMethod(HashSet(), methodDictionary.get(), node, method)
                }
            }
        }
    }

    private fun canRemapMethod(set : HashSet<ClassNode>,startClass : ClassNode,owner : ClassNode,methodNode : MethodNode) : Boolean {
        val tree = getTree(owner.name)

        if (!set.contains(owner)) {
            set.add(owner)

            if (methodMapping.containsKey(methodKey(methodNode,owner))) {
                return false
            }

            if (startClass.name != owner.name && tree.isLibNode) {
                for (method in owner.methods) {
                    if (method.name == methodNode.name && method.desc == methodNode.desc) {
                        return false
                    }
                }
            }

            for (superClass in tree.superClasses) {
                if (!canRemapMethod(set,startClass,superClass,methodNode)) {
                    return false
                }
            }

            for (subClass in tree.subClasses) {
                if (!canRemapMethod(set,startClass,subClass,methodNode)) {
                    return false
                }
            }
        }

        return true
    }

    private fun remapMethod(set : HashSet<ClassNode>,newName : String,owner : ClassNode,methodNode: MethodNode) {
        val tree = getTree(owner.name)

        if (!tree.isLibNode && !set.contains(owner)) {
            methodMapping[methodKey(methodNode,owner)] = newName

            set.add(owner)

            for (superClass in tree.superClasses) {
                remapMethod(set,newName,superClass,methodNode)
            }

            for (subClass in tree.subClasses) {
                remapMethod(set,newName,subClass,methodNode)
            }
        }
    }

    private fun isExclude(node : ClassNode) : Boolean {
        for (s in exclude) {
            if (node.name.startsWith(s)) {
                return true
            }
        }

        return false
    }

    private fun methodKey(method : MethodNode,node : ClassNode) : String {
        return "${node.name}.${method.name}${method.desc}"
    }

    private fun getTree(name : String) : ClassTree {
        if (!classTreeMap.containsKey(name)) {
            buildTree(Obfuscator.Instance.getClassNode(name),null)
        }

        return classTreeMap[name]!!
    }

    private fun buildTree(node : ClassNode, subNode : ClassNode?) {
        if (!classTreeMap.containsKey(node.name)) {
            val tree = ClassTree(node, Obfuscator.Instance.isLibNode(node))

            if (node.superName != null) {
                val superNode = Obfuscator.Instance.getClassNode(node.superName)

                tree.superClasses.add(superNode)

                buildTree(superNode, node)
            }

            if (!node.interfaces.isNullOrEmpty()) {
                for (face in node.interfaces) {
                    val faceNode = Obfuscator.Instance.getClassNode(face)

                    tree.superClasses.add(faceNode)

                    buildTree(faceNode, node)
                }
            }

            classTreeMap[node.name] = tree
        }

        if (subNode != null) {
            classTreeMap[node.name]!!.subClasses.add(subNode)
        }
    }

    private fun remap(node : ClassNode) {
        if (mapping.containsKey(node.name)) return

        if (node.name.contains("$")) { //先找到OuterClass
            remap(Obfuscator.Instance.getClassNode(findOuterClass(node)))
        }

        if (!node.name.contains("$")) {
            mapping[node.name] = dictionary.get()
        } else {
            findInnerName(node).toIntOrNull().run {
                if (this == null) {
                    mapping[node.name] = "${mapping[findOuterClass(node)]}$${dictionary.get()}"
                } else {
                    mapping[node.name] = "${mapping[findOuterClass(node)]}$${this}"
                }
            }
        }
    }

    private fun findOuterClass(node : ClassNode) : String {
        var outerClass : String? = node.outerClass

        if (outerClass == null) {
            val split = node.name.split("$")

            val builder = StringBuilder()

            for (i in split.indices) {
                if (i != split.size - 1) {
                    builder.append(split[i]).append("$")
                }
            }

            outerClass = builder.toString().run {
                return@run substring(0,length - 1)
            }
        }

        return outerClass
    }

    private fun findInnerName(node : ClassNode) : String {
        val split = node.name.split("$")

        return split[split.size - 1]
    }

    private fun isEnumMethod(className : String,classSuperName : String?,methodName : String,methodDesc : String) : Boolean {
        return classSuperName == "java/lang/Enum" && ((methodName == "values" && methodDesc == "()[L${className};") || (methodName == "valueOf" && methodDesc == "(Ljava/lang/String;)L${className};"))
    }

    private fun invalidMethodName(name : String,desc : String) : Boolean {
        return (name == "main" && desc == "([Ljava/lang/String)V") || name == "<init>" || name == "<clinit>"
    }

    override fun onStart(obfuscator: Obfuscator) {
        transform()

        print("Writing mapping")

        FileOutputStream("Mapping.txt").bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            val lineSeparator = System.lineSeparator()

            for (entry in mapping) {
                val className = entry.key
                val classNewName = entry.value
                val node = Obfuscator.Instance.getClassNode(className)

                writer.write("$className -> $classNewName$lineSeparator")

                if (node.fields.isNotEmpty()) {
                    writer.write("  Fields:$lineSeparator")

                    for (field in node.fields) {
                        writer.write("      ${field.name} ${field.desc} -> ${fieldMapping["${node.name}.${field.name}${field.desc}"] ?: field.name}$lineSeparator")
                    }
                }

                writer.write("  Methods:$lineSeparator")

                for (method in node.methods) {
                    if (method.name == "<init>" || method.name == "<clinit>") continue

                    writer.write(
                        "      ${method.name} ${method.desc} -> ${
                            methodMapping[methodKey(
                                method,
                                node
                            )] ?: method.name
                        }$lineSeparator"
                    )
                }

                writer.write(lineSeparator)
            }
        }

        print("Transforming")

        val simpleRemapper = GotoRemapper()
        val newClasses = HashMap<String,ClassWrapper>()

        obfuscator.classes.forEach(action = {
            val newNode = ClassNode()
            val classRemapper = ClassRemapper(newNode, simpleRemapper)

            it.value.classNode.accept(classRemapper)

            newNode.sourceFile = ""

            newClasses[newNode.name] = ClassWrapper(newNode,it.value.originalBytes)
        })

        obfuscator.classes.clear()
        obfuscator.classes.putAll(newClasses)

        obfuscator.allClasses.clear()
        obfuscator.allClasses.putAll(obfuscator.classes)
        obfuscator.allClasses.putAll(obfuscator.libClasses)

        mapping.clear()
        fieldMapping.clear()
        methodMapping.clear()
        classTreeMap.clear()
    }

    @Suppress("DuplicatedCode")
    private fun checkParentField(classNode : ClassNode, fieldName : String, fieldDesc : String) : ClassNode? {
        val instance = Obfuscator.Instance

        var node = instance.getClassNode(classNode.superName)

        while (node.name != "java/lang/Object") {
            for (field in node.fields) {
                if (field.name == fieldName && field.desc == fieldDesc) return node
            }

            node = instance.getClassNode(node.superName)
        }

        for (face in classNode.interfaces) {
            node = instance.getClassNode(face)

            for (field in node.fields) {
                if (field.name == fieldName && field.desc == fieldDesc) return node
            }
        }

        return null
    }

    inner class GotoRemapper : SimpleRemapper(mapping) {
        override fun mapFieldName(owner: String, name: String, descriptor : String) : String {
            return fieldMapping["$owner.$name$descriptor"].run {
                if (this == null) { // 也许是父类字段
                    val classNode = Obfuscator.Instance.getClassNode(owner)
                    val checkParentField = checkParentField(classNode, name, descriptor)

                    if (checkParentField != null) {
                        val s = fieldMapping["${checkParentField.name}.$name$descriptor"]

                        if (s != null)
                            return@run s
                    }

                    return@run name
                } else {
                    return@run this
                }
            }
        }

        override fun mapMethodName(owner: String, name: String, descriptor : String) : String {
            if (name.startsWith("<")) return name

            for (baseMethod in baseMethods) {
                if (baseMethod == name + descriptor) {
                    return name
                }
            }

            val node = Obfuscator.Instance.getClassNode(owner)

            if (isEnumMethod(owner,node.superName,name,descriptor)) {
                return name
            }

            return methodMapping["$owner.$name$descriptor"] ?: name
        }
    }

    class ClassTree(val classNode : ClassNode,val isLibNode : Boolean) {
        val superClasses = ArrayList<ClassNode>()
        val subClasses = ArrayList<ClassNode>()
    }
}