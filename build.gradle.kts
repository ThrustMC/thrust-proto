plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("gen/java/src/main/java")
    }
}

dependencies {
    api("com.google.protobuf:protobuf-java:4.34.1")
    api("io.grpc:grpc-stub:1.74.0")
    api("io.grpc:grpc-protobuf:1.74.0")
    api("com.google.api.grpc:proto-google-common-protos:2.45.0")
}

tasks.javadoc {
    isFailOnError = false
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "thrust-proto"
        }
    }
    repositories {
        maven {
            name = "nexus"
            if (version.toString().endsWith("-SNAPSHOT")) {
                url = uri("https://nexus.junhyung.kr/repository/maven-snapshots/")
            } else {
                url = uri("https://nexus.junhyung.kr/repository/maven-releases/")
            }
            credentials {
                username = providers.environmentVariable("NEXUS_USER").orNull
                    ?: providers.gradleProperty("nexus.username").orNull
                password = providers.environmentVariable("NEXUS_PASSWORD").orNull
                    ?: providers.gradleProperty("nexus.password").orNull
            }
        }
    }
}