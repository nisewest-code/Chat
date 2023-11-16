import java.net.URI

plugins {
    id("java")
    id("scala")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.remaideveloper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = URI("https://repo.akka.io/maven")
    }
    maven {
        setUrl("https://plugins.gradle.org/m2/")
    }
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

val scalaBinary = "2.13"

dependencies {
    implementation(platform("com.typesafe.akka:akka-bom_$scalaBinary:2.9.0"))
    implementation ("com.typesafe.akka:akka-cluster-typed_$scalaBinary")
    implementation("com.typesafe.scala-logging:scala-logging_3:3.9.5")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("org.scala-lang:scala-library:2.13.10")
    implementation ("com.typesafe.akka:akka-serialization-jackson_2.13")
    testImplementation("org.scalatest:scalatest_2.11:3.0.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/scala"))
        }
    }
    test {
        scala {
            setSrcDirs(listOf("test/main"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}