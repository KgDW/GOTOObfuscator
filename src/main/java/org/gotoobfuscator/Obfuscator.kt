package org.gotoobfuscator

import org.apache.commons.io.IOUtils
import org.gotoobfuscator.exceptions.MissingClassException
import org.gotoobfuscator.obj.ClassWrapper
import org.gotoobfuscator.obj.Resource
import org.gotoobfuscator.packer.ConstantPacker
import org.gotoobfuscator.packer.Packer
import org.gotoobfuscator.transformer.Transformer
import org.gotoobfuscator.utils.RandomUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicVerifier
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.ThreadPoolExecutor
import java.util.jar.*
import java.util.zip.*

class Obfuscator(private val inputFile : File,private val outputFile : File) {
    companion object {
        lateinit var Instance : Obfuscator
    }

    val classes = HashMap<String,ClassWrapper>()
    val libClasses = ConcurrentHashMap<String,ClassWrapper>()
    val allClasses = HashMap<String,ClassWrapper>()
    val resources = HashMap<String,Resource>()

    private val libraries = ArrayList<File>()
    private val extractZips = ArrayList<String>()
    private val skipClasses = ArrayList<String>()

    private val transformers = ArrayList<Transformer>()

    private var startTime = -1L

    private val packer = Packer()
    private val constantPacker = ConstantPacker()

    private var manifest : Manifest? = null

    var mainClass = ""

    var corruptCRC = false
    var corruptDate = false
    var classFolder = false
    var duplicateResource = false
    var extractorMode = false
    var useComputeMaxs = false
    var multiThreadLoadLibraries = true
    var preVerify = true
    var dictionaryMode = 0  // 0 Alpha , 1 Number , 2 Unicode , 3 Custom
    var dictionaryFile = ""
    var threadPoolSize = 5

    init {
        if (!inputFile.exists()) {
            throw FileNotFoundException(inputFile.absolutePath)
        }

        Instance = this
    }

    private val isLibMap = HashMap<ClassNode,Boolean>()

    fun isLibNode(node : ClassNode) : Boolean {
        val b = isLibMap[node]

        if (b != null) return b

        for (libClass in libClasses.values) {
            if (libClass.classNode == node) {
                isLibMap[node] = true
                return true
            }
        }

        isLibMap[node] = false
        return false
    }

    private val nodeMap = HashMap<String,ClassNode>()

    fun getClassNode(name : String) : ClassNode {
        val fromMap = nodeMap[name]

        if (fromMap != null) return fromMap

        val classWrapper = allClasses.values.find { it.classNode.name == name } ?: throw MissingClassException(name)

        return classWrapper.classNode.also { nodeMap[name] = it }
    }

    fun start() {
        startTime = System.currentTimeMillis()

        loadInput(inputFile)

        val threadPool = Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor

        println("ThreadPoolSize: ${threadPool.corePoolSize}")

        loadLibraries(threadPool)

        while (threadPool.activeCount != 0) {
            Thread.sleep(1)
        }

        threadPool.shutdown()

        allClasses.putAll(classes)
        allClasses.putAll(libClasses)

        transformers.forEach { transformer ->
            println("Start transformer: ${transformer.name}")

            transformer.onStart(this)

            classes.values.forEach { wrapper ->
                transformer.transform(wrapper.classNode)
            }

            transformer.finish(this)
        }

        if (constantPacker.isEnable) {
            classes.forEach {
                constantPacker.accept(it.value.classNode)
            }
        }

        if (preVerify) {
            preVerify()
        }

        writeOutput()

        if (packer.isEnable) {
            packer.writeMapping()
        }
    }

    private fun preVerify() {
        println("Pre verifying")

        val analyzer = Analyzer(BasicVerifier())

        for (classWrapper in classes.values) {
            for (method in classWrapper.classNode.methods) {
                try {
                    analyzer.analyzeAndComputeMaxs(method.name,method)
                } catch (e : Throwable) {
                    e.addSuppressed(Throwable("Found throwable ClassName:${classWrapper.classNode.name} MethodName:${method.name}"))

                    throw e
                }
            }
        }
    }

    private fun loadInput(inputFile : File) {
        val file = JarFile(inputFile)
        val entries = file.entries()

        var loadedResources = 0
        var loadedClasses = 0
        var skippedClasses = 0

        var entry : JarEntry

        while (entries.hasMoreElements()) {
            entry = entries.nextElement()

            when {
                entry.name.endsWith(".class") -> {
                    if (extractorMode || entry.name.endsWith("module-info.class")) {
                        putResource(file,entry)
                        loadedResources++
                    } else {
                        var skip = false

                        ForEach@ for (s in skipClasses) {
                            if (entry.name.startsWith(s)) {
                                putResource(file, entry)
                                skippedClasses++

                                skip = true
                                break@ForEach
                            }
                        }

                        if (!skip) {
                            putClass(file, entry)
                            loadedClasses++
                        }
                    }
                }
                entry.name.equals("META-INF/MANIFEST.MF") -> {
                    println("Manifest found")

                    manifest = file.manifest

                    if (mainClass.isEmpty()) {
                        val mc = manifest!!.mainAttributes[Attributes.Name.MAIN_CLASS]

                        if (mc != null) {
                            mainClass = mc as String

                            println("MainClass found: $mc")
                        }
                    } else {
                        println("MainClass already set as: $mainClass")
                    }

                    loadedResources++
                }
                else -> {
                    putResource(file,entry)
                    loadedResources++
                }
            }
        }

        file.close()

        println("Loaded $loadedClasses classes")
        println("Skipped $skippedClasses classes")
        println("Loaded $loadedResources resources")
    }

    private fun loadLibraries(threadPool : ThreadPoolExecutor) {
        println("Loading libraries MultiThreadLoadLibraries:${multiThreadLoadLibraries}")

        libraries.forEach { libFile ->
            if (libFile.isDirectory) {
                libFile.listFiles()?.forEach {
                    try {
                        if (multiThreadLoadLibraries) {
                            threadPool.execute(SingleLibraryLoader(it))
                        } else {
                            SingleLibraryLoader(it).run()
                        }
                    } catch (e : Throwable) {
                        e.printStackTrace()
                    }
                }
            } else {
                try {
                    if (multiThreadLoadLibraries) {
                        threadPool.execute(SingleLibraryLoader(libFile))
                    } else {
                        SingleLibraryLoader(libFile).run()
                    }
                } catch (e : Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun writeOutput() {
        println("Writing output")

        val jos = JarOutputStream(FileOutputStream(outputFile))

        if (corruptCRC) {
            val field = ZipOutputStream::class.java.getDeclaredField("crc")
            field.isAccessible = true
            field.set(jos, object : CRC32() {
                override fun getValue() : Long {
                    return 0
                }
            })
        }

        if (manifest != null) {
            if (packer.isEnable && mainClass.isNotEmpty()) {
                println("[Packer] Main class set to: org.gotoobfuscator.runtime.GotoMain")

                if (manifest!!.mainAttributes.containsKey(Attributes.Name.MAIN_CLASS)) {
                    manifest!!.mainAttributes.replace(Attributes.Name.MAIN_CLASS, "org.gotoobfuscator.runtime.GotoMain")
                } else {
                    manifest!!.mainAttributes[Attributes.Name.MAIN_CLASS] = "org.gotoobfuscator.runtime.GotoMain"
                }
            }

            val entry = JarEntry("META-INF/MANIFEST.MF")

            handleEntry(entry)

            jos.putNextEntry(entry)
            manifest!!.write(jos)
            jos.closeEntry()

            putDuplicateResource(entry.name,jos)
        }

        if (mainClass.isNotEmpty()) {
            if (packer.isEnable) {
                packer.setupMain(mainClass,jos,this)
            }
        }

        resources.values.forEach(
            action = { resource ->
                if (resource.name.endsWith("/") && resources.values.find { it != resource && !it.name.endsWith("/") && it.name.startsWith(resource.name) } == null) {
                    return@forEach
                }

                val entry = JarEntry(resource.name)

                handleEntry(entry)

                jos.putNextEntry(entry)
                jos.write(resource.data)
                jos.closeEntry()

                putDuplicateResource(entry.name,jos)
            }
        )

        classes.values.forEach(
            action = {
                val entry = if (packer.isEnable)
                    packer.handleEntry(it)
                else JarEntry(
                    if (classFolder)
                        "${it.name}.class/"
                    else
                        "${it.name}.class"
                )

                handleEntry(entry)

                try {
                    jos.putNextEntry(entry)
                } catch (e : Throwable) {
                    println("Writing class ${it.name} error")
                    throw e
                }

                try {
                    jos.write(
                        if (packer.isEnable)
                            packer.handleBytes(it.toByteArray(useComputeMaxs))
                        else
                            it.toByteArray(useComputeMaxs)
                    )
                } catch (e : Throwable) {
                    e.printStackTrace()
                    println("Write class ${it.name} error try use original bytes")

                    jos.write(it.originalBytes)
                }

                jos.closeEntry()

                putDuplicateResource(entry.name,jos)
            }
        )

        if (constantPacker.isEnable) {
            run {
                val b = constantPacker.build()

                val entry = JarEntry("Const")

                handleEntry(entry)

                jos.putNextEntry(entry)
                jos.write(b)
                jos.closeEntry()

                putDuplicateResource(entry.name,jos)
            }

            run {
                val b = constantPacker.buildClass()

                @Suppress("SpellCheckingInspection")
                val entry = JarEntry("org/gotoobfuscator/runtime/Const.class")

                handleEntry(entry)

                jos.putNextEntry(entry)
                jos.write(b)
                jos.closeEntry()

                putDuplicateResource(entry.name,jos)
            }
        }

        if (corruptCRC) {
            val field = ZipOutputStream::class.java.getDeclaredField("crc")
            field.isAccessible = true
            field.set(jos, CRC32())
        }

        for (extractZip in extractZips) {
            var zipFile : ZipFile? = null

            try {
                val file = File(extractZip)

                if (!file.exists()) {
                    println("WARNING: zip not exists: ${file.absolutePath}")
                    continue
                }

                println("Extracting zip: ${file.absolutePath}")

                zipFile = ZipFile(file)

                for (entry in zipFile.entries()) {
                    try {
                        val inputStream = zipFile.getInputStream(entry)

                        jos.putNextEntry(entry)
                        jos.write(IOUtils.toByteArray(inputStream))
                        jos.closeEntry()

                        putDuplicateResource(entry.name, jos)
                    } catch (e : ZipException) {
                        if (e.message!!.startsWith("duplicate entry")) {
                            println("Extract entry failed reason: ${e.message}")
                        }
                    }
                }
            } catch (e : Throwable) {
                e.printStackTrace()
                println("WARNING: extract zip failed! ${e.message}")
            } finally {
                zipFile?.close()
            }
        }

        jos.finish()
        jos.close()

        val time = System.currentTimeMillis() - startTime

        println("用时${time}ms 即${time / 1000.0}s")
    }

    fun handleEntry(entry : ZipEntry) {
        if (corruptDate) {
            val time = ThreadLocalRandom.current().nextLong(0L,Long.MAX_VALUE)

            entry.time = time
            entry.creationTime = FileTime.fromMillis(time)
            entry.lastAccessTime = FileTime.fromMillis(time)
            entry.lastModifiedTime = FileTime.fromMillis(time)
        }
    }

    private fun putDuplicateResource(entryName : String,jos : JarOutputStream) {
        if (!duplicateResource) return

        for (i in 1..ThreadLocalRandom.current().nextInt(2,10)) {
            val entry = JarEntry("${entryName}${"\u0000".repeat(i)}")

            handleEntry(entry)

            jos.putNextEntry(entry)
            jos.write(RandomUtils.randomString(100,RandomUtils.UNICODE).toByteArray(StandardCharsets.UTF_8))
            jos.closeEntry()
        }
    }

    private fun putClass(jarFile : JarFile,entry: JarEntry) {
        val stream = jarFile.getInputStream(entry)
        classes[entry.name] = ClassWrapper(IOUtils.toByteArray(stream))
        stream.close()
    }

    private fun putResource(jarFile : JarFile,entry: JarEntry) {
        val stream = jarFile.getInputStream(entry)
        resources[entry.name] = Resource(entry.name,IOUtils.toByteArray(stream))
        stream.close()
    }

    fun addLibraries(vararg files : String) {
        for (fileName in files) {
            addLibraries(File(fileName))
        }
    }

    fun addLibraries(vararg files : File) {
        libraries.addAll(files)
    }

    fun addTransformers(vararg transformers : Transformer) {
        this.transformers.addAll(transformers)
    }

    fun addTransformers(transformers : List<Transformer>) {
        this.transformers.addAll(transformers)
    }

    fun addExtractZips(vararg zipFiles : String) {
        this.extractZips.addAll(zipFiles)
    }

    fun addExtractZips(zipFiles : List<String>) {
        this.extractZips.addAll(zipFiles)
    }

    fun addSkipClasses(skipClasses : List<String>) {
        this.skipClasses.addAll(skipClasses)
    }

    fun addSkipClasses(vararg skipClasses : String) {
        this.skipClasses.addAll(skipClasses)
    }

    fun setPackerEnable(enable : Boolean) {
        packer.isEnable = enable
    }

    fun setConstantPackerEnable(enable : Boolean) {
        constantPacker.isEnable = enable
    }

    private inner class SingleLibraryLoader(private val libFile : File) : Runnable {
        override fun run() {
            var loadedClasses = 0

            JarFile(libFile).use { file ->
                for (entry in file.entries()) {
                    val name = entry.name

                    if (name.endsWith(".class")) {
                        file.getInputStream(entry).use { stream ->
                            val data = IOUtils.toByteArray(stream)

                            val classReader = ClassReader(data)
                            val classNode = ClassNode()

                            classReader.accept(classNode,0)

                            libClasses[name] = ClassWrapper(classNode,data)

                            loadedClasses++
                        }
                    }
                }
            }

            println("Loaded $loadedClasses classes from ${libFile.absolutePath}")
        }
    }
}