# Keep the launcher entry point (referenced from the manifest only).
-keep class bin.nt.plus.MainActivity { *; }

# Strip verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
