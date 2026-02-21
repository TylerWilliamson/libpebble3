import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.TylerWilliamson"
            artifactId = "libpebble3"
            version = "1.1.1"
        }
    }
}

room {
    schemaDirectory("schema")
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "io.rebble.libpebblecommon"
    defaultConfig {
        minSdk = 26
        lint.targetSdk = compileSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.valueOf("VERSION_${libs.versions.jvm.toolchain.get()}")
        targetCompatibility = JavaVersion.valueOf("VERSION_${libs.versions.jvm.toolchain.get()}")
    }

    kotlin {
        jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    }

    buildTypes {
        release {
            consumerProguardFiles("consumer-rules.pro")
        }
    }
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
    }

    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
        commonMain {
            kotlin {
                // Include ksp-generated common code (from our :blobdgen processor)
                srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
        }
        commonMain.dependencies {
            api(libs.coroutines)
            implementation(libs.serialization)
            implementation(libs.kermit)
            implementation(libs.room.runtime)
            api(libs.room.paging)
            implementation(libs.sqlite.bundled)
            api(libs.kotlinx.io.core)
            implementation(libs.kotlinx.io.okio)
            implementation(libs.okio)
            implementation(libs.kable)
            implementation(libs.kmpio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.websockets)
            api(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(compose.ui)
            implementation(project(":blobannotations"))
            implementation(libs.settings)
            implementation(libs.settings.serialization)
            implementation(libs.uri)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.pebblekit)
        }

        jvmMain.dependencies {
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
            implementation(libs.ktor.websockets)
            implementation(libs.ktor.cio)
            implementation(libs.ktor.client.okhttp)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.rules)
            implementation(libs.androidx.monitor)
        }
    }
}

// Otherwise it doesn't trigger our blobdbgen processor when compiling code
// https://github.com/google/ksp/issues/567
tasks.withType<KotlinCompilationTask<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
afterEvaluate {
    tasks.named("kspDebugKotlinAndroid") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
    tasks.named("kspReleaseKotlinAndroid") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
    tasks.named("jvmSourcesJar") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

dependencies {
//    add("kspCommonMainMetadata", libs.room.compiler)
//    add("kspJvm", libs.room.compiler)
    add("kspCommonMainMetadata", project(":blobdbgen"))
    add("kspAndroid", libs.room.compiler)
}