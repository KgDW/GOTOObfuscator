package org.gotoobfuscator.packer

import org.gotoobfuscator.Obfuscator
import org.gotoobfuscator.obj.ClassWrapper
import org.gotoobfuscator.runtime.GotoMain
import org.gotoobfuscator.transformer.SpecialTransformer
import org.gotoobfuscator.transformer.transformers.JunkCode
import org.gotoobfuscator.transformer.transformers.NumberEncryptor
import org.gotoobfuscator.transformer.transformers.StringEncryptor
import org.gotoobfuscator.utils.EncodeUtils
import org.gotoobfuscator.utils.RandomUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max

class Packer : SpecialTransformer("Packer") {
    private val mapping = HashMap<String,String>()
    private val key = RandomUtils.randomString(100,RandomUtils.UNICODE).encodeToByteArray()

    fun setupMain(mainClass : String,jos : JarOutputStream,instance : Obfuscator) {
        val node = ClassNode()
        val reader = ClassReader(Packer::class.java.getResourceAsStream("/org/gotoobfuscator/runtime/GotoMain.class"))

        reader.accept(node,0)

        node.methods.find { it.name.equals("main") }!!.instructions.forEach(action = {
            if (it is LdcInsnNode && it.cst is String) {
                when (it.cst.toString()) {
                    "%{MAIN_CLASS_BASE64}%" -> {
                        it.cst = String(Base64.getEncoder().encode(mainClass.encodeToByteArray()),StandardCharsets.UTF_8)
                    }
                    "%{KEY_BASE64}%" -> {
                        it.cst = String(Base64.getEncoder().encode(key),StandardCharsets.UTF_8)
                    }
                }
            }
        })

        StringEncryptor().transform(node)
        NumberEncryptor().transform(node)
        JunkCode().transform(node)

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        node.accept(writer)

        val entry = JarEntry("org/gotoobfuscator/runtime/GotoMain.class")

        instance.handleEntry(entry)
        jos.putNextEntry(entry)
        jos.write(writer.toByteArray())
        jos.closeEntry()
    }

    fun handleEntry(wrapper : ClassWrapper) : JarEntry {
        val encrypt = GotoMain.sha256(wrapper.classNode.name.toByteArray(StandardCharsets.UTF_8))

        mapping[wrapper.classNode.name] = encrypt

        return JarEntry(encrypt)
    }

    fun handleBytes(b : ByteArray) : ByteArray {
        return EncodeUtils.desEncode(b, key)
    }

    fun writeMapping() {
        FileOutputStream("PackerMapping.txt").use { fos ->
            mapping.forEach(action = {
                fos.write("${it.key} -> ${it.value}${System.lineSeparator()}".toByteArray(StandardCharsets.UTF_8))
            })
        }
    }
}