import java.net.URI
import java.security.MessageDigest
import java.time.Instant

abstract class FetchProxyJarTask : DefaultTask() {
    @get:Input
    abstract val sourceUrl: Property<String>

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun fetch() {
        val dest = outputFile.get().asFile
        val expected = expectedSha256.get().lowercase()
        dest.parentFile.mkdirs()

        if (dest.exists() && sha256(dest) == expected) {
            logger.lifecycle("Using cached MCP proxy JAR: ${dest.name}")
            return
        }

        logger.lifecycle("Downloading MCP proxy JAR from ${sourceUrl.get()}")
        dest.delete()
        val bytes = URI(sourceUrl.get()).toURL().readBytes()
        dest.writeBytes(bytes)

        val actual = sha256(dest)
        if (actual != expected) {
            dest.delete()
            throw GradleException(
                "MCP proxy JAR checksum mismatch (expected $expected, got $actual). " +
                    "Place a matching jar at ${dest.absolutePath} or update mcp.proxy.sha256."
            )
        }
    }

    private fun sha256(file: java.io.File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

abstract class EmbedProxyJarTask : DefaultTask() {
    @get:InputFile
    abstract val shadowJarFile: RegularFileProperty

    @get:InputFile
    abstract val proxyJarFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun embedJar() {
        val shadowJar = shadowJarFile.get().asFile
        val proxyJar = proxyJarFile.get().asFile

        execOperations.exec {
            commandLine("jar", "uf", shadowJar.absolutePath, "-C", proxyJar.parentFile.absolutePath, proxyJar.name)
        }

        logger.lifecycle("Embedded proxy JAR into ${shadowJar.name}")
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    java
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()

val mcpProxyJar = layout.buildDirectory.file("proxy/mcp-proxy-all.jar")

dependencies {
    compileOnly(libs.burp.montoya.api)

    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mcp.kotlin.sdk)

    testImplementation(libs.bundles.test.framework)
    testImplementation(libs.bundles.ktor.test)
    testImplementation(libs.burp.montoya.api)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("java.toolchain.version").get().toInt()))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("java.toolchain.version").get().toInt()))
    }

    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict"
        )
    }
}

application {
    mainClass.set("net.portswigger.mcp.ExtensionBase")
}

tasks {
    val fetchProxyJar = register<FetchProxyJarTask>("fetchProxyJar") {
        group = "build"
        description =
            "Downloads the PortSwigger mcp-proxy fat JAR (stdio bridge) into build/proxy/ — not built by this project"
        sourceUrl.set(providers.gradleProperty("mcp.proxy.url"))
        expectedSha256.set(providers.gradleProperty("mcp.proxy.sha256"))
        outputFile.set(mcpProxyJar)
    }

    test {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        dependsOn(fetchProxyJar)

        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "zamdevio",
                    "Built-By" to System.getProperty("user.name"),
                    "Built-Date" to Instant.now().toString(),
                    "Built-JDK" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                        System.getProperty("java.vm.version")
                    })",
                    "Created-By" to "Gradle ${gradle.gradleVersion}"
                )
            )
        }


        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/NOTICE*")
        exclude("META-INF/LICENSE*")
        exclude("module-info.class")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    register<EmbedProxyJarTask>("embedProxyJar") {
        group = "build"
        description = "Embeds the MCP proxy JAR into the shadow JAR"
        dependsOn(shadowJar, fetchProxyJar)
        shadowJarFile.set(shadowJar.flatMap { it.archiveFile })
        proxyJarFile.set(mcpProxyJar)
    }

    build {
        dependsOn(shadowJar)
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

tasks.wrapper {
    gradleVersion = "9.2.0"
    distributionType = Wrapper.DistributionType.BIN
}
