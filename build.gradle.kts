plugins {
  id("java")
  id("application")
}

group = "org.labs"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  val assertjCoreVersion = "3.25.3"

  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core:$assertjCoreVersion")
}

tasks.test {
  useJUnitPlatform()
}

application {
  mainClass = "org.labs.Main"
}

sourceSets {
  main {
    java.srcDir("src")
  }
  test {
    java.srcDir("test")
    resources.srcDir("test/resources")
  }
}