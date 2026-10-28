# Sonic 的 R8 规则。
#
# 原则：**不写"以防万一"的宽泛 keep**。Hilt / Room / Compose / Media3 / Coil / kotlinx.serialization
# 都自带 consumer 规则，AGP 会把它们合并进来；这里只补两类东西：
#   1. 崩溃栈可读性（行号）；
#   2. 项目里确实靠反射/资源名查找的入口。
#
# 想确认某条规则是不是多余的：Android Studio 的 R8 Configuration Analyzer
# （Build > Analyze R8 Configuration）会指出被其他规则覆盖的条目。

# 崩溃栈要能读：R8 会重命名标识符，但保留行号后 retrace 仍能定位到源码行。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 桌面小组件（Glance）的 receiver 由系统按清单实例化，运行时会用类名反查；
# Glance 的 ActionCallback 也是按类名从广播里恢复的。
-keep class com.cycling.presentation.widget.SonicWidgetReceiver { *; }
-keep class com.cycling.presentation.widget.SonicWidgetAction { *; }

# Room 的 schema 校验会读 @Database 类的注解；Room 自带规则覆盖了 DAO/Entity，
# 但版本号取自 @Database 注解本身，单独保一下注解属性。
-keepattributes *Annotation*

# QuickJS 绑定层靠 JNI 按「类名 + 方法名」解析 native 符号，R8 重命名会让 libquickjs.so 找不到符号。
# 只保库本身的类名；我们自己实现 FunctionBinding 的 lambda 不需要 keep（不是按名字反射的入口）。
-keep class com.dokar.quickjs.** { *; }
-dontwarn com.dokar.quickjs.**

# jaudiotagger 的 ID3v2 帧体是按 Class.forName("org.jaudiotagger.tag.id3.framebody." + 帧id)
# 造出来的，R8 重命名后丢的不只是一个类，而是整类帧（APIC 封面、USLT 歌词、TXXX ReplayGain 都在
# 这层）。只保这一层，其余靠正常可达性分析。
-keep class org.jaudiotagger.tag.id3.framebody.** { *; }
# 封面解码那几条路径引用 AWT / ImageIO / Swing，Android 上没有这些包；Sonic 的封面走 Coil +
# MediaStore，不会实例化它们，缺类不必报错。
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
