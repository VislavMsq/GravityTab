# ✅ Основна оптимізація
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# ✅ Максимальна обфускація (словарі)
-obfuscationdictionary obfuscation.txt
-classobfuscationdictionary class_obfuscation.txt
-packageobfuscationdictionary package_obfuscation.txt

# ✅ Перенесення всіх класів у корінь пакету (зменшує зрозумілість структури)
-repackageclasses 'com.FDGEntertain'

# ✅ Видалення всіх логів
-assumenosideeffects class android.util.Log {
    public static *** *(...);
}

# ✅ Збереження Android компонентів (щоб додаток не зламався)
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider