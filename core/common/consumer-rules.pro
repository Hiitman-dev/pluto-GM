# Consumer ProGuard rules for core:common
# Keep the Result sealed class hierarchy — it's accessed via reflection
# in some test runners.
-keep class com.pluto.core.common.Result { *; }
-keep class com.pluto.core.common.Result$* { *; }
-keep class com.pluto.core.common.ApiException { *; }
-keep class com.pluto.core.common.ApiException$* { *; }
