package com.music.bitchord.data

/** The fork owns its releases. Benchmarks must never offer an upstream replacement APK. */
internal fun tvReleaseApiUrl(buildType: String): String? =
    if (buildType == "benchmark") null else "https://api.github.com/repos/gggg-unit/ChordTV/releases/latest"
