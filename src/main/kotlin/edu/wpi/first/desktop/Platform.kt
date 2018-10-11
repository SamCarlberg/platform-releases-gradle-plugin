package edu.wpi.first.desktop

enum class Platform(val platformName: String) {
    WIN32("win32"),
    WIN64("win64"),
    MAC("mac64"),
    LINUX("linux64");

    companion object {
        fun forName(platformName: String): Platform {
            return values().find { it.platformName == platformName } ?: throw NoSuchElementException(platformName)
        }

        /**
         * The list of supported platforms.
         */
        val platforms: List<Platform> by lazy {
            Platform.values().asList()
        }
    }
}
