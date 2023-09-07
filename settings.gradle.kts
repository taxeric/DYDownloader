import java.net.URI

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
//        maven("https://developer.huawei.com/repo/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        maven {
//            url = URI("https://developer.huawei.com/repo/")
//        }
        google()
        mavenCentral()
    }
}

rootProject.name = "DYDownloader"
include(":app")
include(":lib_js_bridge")
