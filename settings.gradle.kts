pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.ghostscript.com")
    }
}

rootProject.name = "nexora-office"

include(
    ":app",
    ":core:common",
    ":core:model",
    ":core:designsystem",
    ":core:navigation",
    ":core:database",
    ":core:data",
    ":feature:dashboard",
    ":feature:filemanager",
    ":feature:editor"
)
