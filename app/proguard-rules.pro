# Ek Chhoan R8 rules (AGP 9 runs R8 in full mode).
#
# kotlinx.serialization ships its own consumer rules (META-INF/com.android.tools/r8*) that keep the
# generated $serializer classes and Companion.serializer() of every @Serializable class. The rules
# below are a small, explicit safety net for our own model so the stored family list (DataStore JSON)
# can always be decoded after shrinking. Serial names are compile-time strings, so obfuscating the
# class/field names themselves is safe.

-keepclassmembers @kotlinx.serialization.Serializable class com.pntapps.ekchhoan.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.pntapps.ekchhoan.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pntapps.ekchhoan.data.**$$serializer { *; }

# Keep line numbers for readable Play Console crash stacks (Android vitals is collected by Play, not by us).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
