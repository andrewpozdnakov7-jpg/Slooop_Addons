-keep class io.dashchan2.addon.translation.** { *; }
-keep class io.dashchan2.addon.googletranslate.GoogleTranslationService { *; }

# Firebase component discovery instantiates ML Kit registrars through reflection.
# The dependency only preserves registrar class names, so keep their public
# no-argument constructors as well when R8 full mode is enabled.
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
	public <init>();
}
