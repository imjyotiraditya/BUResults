# For AboutLibraries proper functioning
-keep class **.R$* {
    <fields>;
}

-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-dontwarn android.view.DisplayListCanvas
-dontwarn android.view.RenderNode
-dontwarn com.google.accompanist.insets.ComposeInsets
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn org.openjsse.**