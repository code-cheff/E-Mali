# Keep TensorFlow Lite classes (they are accessed via JNI/reflection).
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
