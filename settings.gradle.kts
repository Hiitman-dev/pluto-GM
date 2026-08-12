pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LibVLC snapshots for advanced playback if needed
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PLUTO"

include(":app")

// Core modules
include(":core:common")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:network")
include(":core:database")
include(":core:media")
include(":core:navigation")
include(":core:notifications")
include(":core:download")

// Feature modules
include(":feature:splash")
include(":feature:auth")
include(":feature:home")
include(":feature:search")
include(":feature:details")
include(":feature:player")
include(":feature:downloads")
include(":feature:favorites")
include(":feature:history")
include(":feature:notifications")
include(":feature:settings")
