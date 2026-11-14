# 默认混淆规则之外的项目规则
# kotlinx-serialization 自带 consumer 规则，这里显式保留领域模型的 serializer 以防万一
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.example.watchgomoku.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.watchgomoku.** {
    kotlinx.serialization.KSerializer serializer(...);
}
