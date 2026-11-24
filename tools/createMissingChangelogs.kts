/**
 * This script will duplicate the latest english changelog in all the locales without it.
 */
import java.io.File

/* ------------------- settings ------------------- */

val DEFAULT_LOCALE = "en-US"

val fastlane = File("fastlane/metadata/android")

val versionCode = File(fastlane, "$DEFAULT_LOCALE/changelogs").listFiles()!!.map { it.nameWithoutExtension }.maxBy { it.toInt() }

val changelog = File(fastlane, "$DEFAULT_LOCALE/changelogs/$versionCode.txt").readText()

fastlane.list()?.forEach { locale ->
    val file = File(fastlane, "$locale/changelogs/$versionCode.txt")
    if (!file.exists()) {
        println("Creating $locale changelog")
        file.parentFile.mkdirs()
        file.writeText(changelog)
    }
}