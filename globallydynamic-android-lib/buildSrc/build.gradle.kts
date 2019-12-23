buildscript {
    apply(from = rootProject.file("../deps.gradle"))
}

plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("feature-plugin") {
            id = "aggregate-javadoc"
            implementationClass = "com.jeppeman.globallydynamic.AggregateJavadocPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

val deps: Map<String, Any> by extra
val android: Map<String, Any> by deps
val kotlin: Map<String, Any> by deps

dependencies {
    implementation(android["gradle_plugin"] ?: error(""))
    implementation(kotlin["plugin"] ?: error(""))
}
