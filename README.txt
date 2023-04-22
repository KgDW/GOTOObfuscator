=== Goto Obfuscator === by 亚蓝

java -jar GotoObfuscator.jar build <your config name> to output an empty config.
java -jar GotoObfuscator.jar handle <your config name> to obfuscate with this config.

The config format is JSON.

Plugin introduction:
Create a folder named "GotoPlugins" in the Goto working directory and put the plugins in it.
Note: Goto only searches for plugins with a .jar extension.

Below is an introduction to the objects:
InputFile -> Input jar file, e.g. "input.jar" "obf/input.jar"
OutputFile -> Output jar file, e.g. "output.jar" "obf/output.jar"
MainClass -> Main class. If empty, it will automatically find the main class. Separator is ".", e.g. "net.test.Main"
ClassRenameDictionaryFile -> Custom dictionary file path, one word per line, e.g. "MyDictionaryFile.txt" "MyFolder/MyDictionaryFile.txt" "C:\MyFolder\MyDictionaryFile.txt"
Libraries -> Either the folder containing external libraries or their names. If a folder is specified, files in the folder will be searched and used as libraries. Otherwise, it will be treated as a library directly, e.g. "MyLibraries/" "MyLib.jar"
ExtractZips -> Extract zip or jar files to the output file, e.g. "rt.jar" "MyLibrary.jar" "MysteriousImage.zip"
SkipClasses -> Classes not to be loaded, using "startsWith" for judgment. The separator is "/", e.g. "net/" "net/Main"
ClassRenameExclude -> Classes to be skipped by ClassRename, using "startsWith" for judgment. The separator is "/", e.g. "net/" "net/Main"
ClassRenameDictionaryMode -> ClassRename dictionary mode: 0 is Alpha (a b c aa ab ac ba bb bc...), 1 is Number (0 1 2 00 01 02...), 2 is Arabic characters, 3 is custom dictionary
ThreadPoolSize -> Size of the thread pool, default is 5
MultiThreadLoadLibraries -> Use multithreading to load external libraries, enabled by default (Note: The number of threads created depends on the number of libraries)
PreVerify -> Enabled by default
CorruptCRC -> Corrupt CRC
CorruptDate -> Corrupt date
ClassFolder -> Turn class files into folders
Packer -> Pack all classes for loading, default is to generate "org/gotoobfuscator/runtime/GotoMain.class" as the loader
ConstantPacker -> Pack all strings and numbers into "Const" file, accessed through a specific loader
ExtractorMode -> Treat classes as resources, suitable for those who only use CorruptCRC, CorruptDate, Crasher, etc.
UseComputeMaxs -> Use COMPUTE_MAXS directly as the parameter for ClassWriter
ClassRename -> Rename classes, fields, and methods (Note: Very slow, be patient)
StringEncryptor -> String encryption
HideCode -> Hide code from decompilation, making it impossible to generate decompiled code
NumberEncryptor -> Number encryption
JunkCode -> Generate junk code
SourceRename -> Rename the class's "SourceName" & "SourceDebug"
BadAnnotation -> Generate bad Annotations
Crasher -> Crash decompiler
InvalidSignature -> Invalid signature
VariableRename -> Rename local variables