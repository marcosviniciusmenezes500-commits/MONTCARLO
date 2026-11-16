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
    }
}

rootProject.name = "DroidScope"
include(
    ":app",
    ":core",
    ":core-ui",
    ":core-database",
    ":core-domain",
    ":feature-dashboard",
    ":feature-battery",
    ":feature-benchmark",
    ":feature-hardware",
    ":feature-cpu",
    ":feature-memory",
    ":feature-storage",
    ":feature-sensors",
    ":feature-network",
    ":feature-display",
    ":feature-diagnostics",
    ":feature-history",
    ":feature-settings"
)
