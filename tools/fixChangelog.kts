/**
 * This script will prepare a triplet folder for publication. In particular it will:
 * - [optional] Create missing locale files (copied from the default locale)
 * - Add missing version changelogs from the default locale
 * - [optional] Trim changelogs to MAX_LENGTH
 */
import java.io.File

/* ------------------- settings ------------------- */

val RELEASE_NOTES = "app/src/main/play/release-notes"
val LISTINGS = "app/src/main/play/listings"
val FILE = "default.txt"
val DEFAULT = "en-US"
val MAX_LENGTH = 500

val DELIM = "\n\n"

val PUBLISH = args.contains("publish")

/* ------------------- utils ------------------- */

/** Returns the default versions */
val defaultVersions = File(File(RELEASE_NOTES, DEFAULT), FILE).let { file ->
    file.readVersions.also { versions ->
        file.writeText(versions.getFixedVersions(DEFAULT))
    }
}

/** Reads the versions from a versions file */
val File.readVersions
    get() = readText().replace("\r\n", "\n").split(Regex(DELIM))

/** Returns the valid versions from a versions list */
fun List<String>.getFixedVersions(locale: String) = runningReduce { acc, s -> acc + DELIM + s }
    .lastOrNull { it.length <= MAX_LENGTH } ?: throw Exception("There is no changelog with less than $MAX_LENGTH chars for locale $locale")


/* ------------------- main ------------------- */

// from all listings
File(LISTINGS).list().orEmpty()
    // get the changelog
    .associateWith { File(File(RELEASE_NOTES, it), FILE) }.entries
    .forEach { (locale, file) ->
        // check if file exists
        if (!file.exists()) {
            if (PUBLISH) {
                // file doesn't exists and we want to publish
                println("Creating $file")
                file.parentFile.mkdirs()
                file.writeText(defaultVersions.getFixedVersions(DEFAULT))
            }
            return@forEach
        }

        val versions = file.readVersions

        val fixedVersions = defaultVersions.map { englishVersion ->
            versions.find { version ->
                version.lines().first() == englishVersion.lines().first()
            } ?: englishVersion
        }.run { if (PUBLISH) getFixedVersions(locale) else joinToString(DELIM) }

        println("Fixing $locale")
        file.writeText(fixedVersions)
    }
