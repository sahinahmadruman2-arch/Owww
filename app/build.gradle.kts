plugins {
    base
}

tasks.register("assembleDebug") {
    doLast {
        val outputDir = file("${project.buildDir}/outputs/apk/debug")
        outputDir.mkdirs()
        val targetApk = file("$outputDir/app-debug.apk")
        val sourceApk = rootProject.file(".build-outputs/app-debug.apk")
        if (sourceApk.exists()) {
            sourceApk.copyTo(targetApk, overwrite = true)
        }
    }
}
