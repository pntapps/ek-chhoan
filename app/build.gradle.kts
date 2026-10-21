import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Optional release signing from keystore.properties (git-ignored). Without it, release builds are unsigned.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.pntapps.ekchhoan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pntapps.ekchhoan"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ---------------------------------------------------------------------------------------------
// Google Play compliance guard (spec §5): fail the build if the MERGED manifest (ours + every
// library's) requests any permission outside the allow-list. Runs automatically before
// assemble<Variant>, bundle<Variant>, install<Variant> and check; run alone with
// ./gradlew :app:checkForbiddenPermissionsDebug (or ...Release).
// ---------------------------------------------------------------------------------------------
@CacheableTask
abstract class CheckForbiddenPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowedPermissions: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val androidNs = "http://schemas.android.com/apk/res/android"
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val doc = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        val requested = sortedSetOf<String>()
        for (tag in listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val name = (nodes.item(i) as org.w3c.dom.Element).getAttributeNS(androidNs, "name")
                if (name.isNotBlank()) requested += name
            }
        }
        val allowed = allowedPermissions.get()
        val forbidden = requested - allowed
        report.get().asFile.writeText(
            buildString {
                appendLine("Merged manifest: ${mergedManifest.get().asFile}")
                requested.forEach { appendLine((if (it in allowed) "OK        " else "FORBIDDEN ") + it) }
            }
        )
        if (forbidden.isNotEmpty()) {
            throw GradleException(
                "Forbidden permission(s) in merged manifest: ${forbidden.joinToString()}.\n" +
                    "Only $allowed are allowed (Play compliance, spec §5). Find the library that adds it in " +
                    "app/build/outputs/logs/manifest-merger-*-report.txt and strip it with " +
                    "<uses-permission android:name=\"...\" tools:node=\"remove\" /> in AndroidManifest.xml."
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val checkTask = tasks.register<CheckForbiddenPermissionsTask>("checkForbiddenPermissions$cap") {
            group = "verification"
            description = "Fails if the ${variant.name} merged manifest requests a non-allow-listed permission."
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            allowedPermissions.addAll(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.CALL_PHONE",
                "android.permission.VIBRATE",
            )
            // androidx.core (1.9+) declares and uses a signature-level permission scoped to our own
            // applicationId so ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED) works below
            // API 33. It grants nothing to other apps and is not a user-visible permission.
            allowedPermissions.add(variant.applicationId.map { "$it.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" })
            report.set(layout.buildDirectory.file("reports/permissions/${variant.name}-permissions.txt"))
        }
        // Lazy name filter (Gradle 8.6+): does not realize unrelated tasks; configuration-cache safe.
        tasks.named { it == "assemble$cap" || it == "bundle$cap" || it == "install$cap" || it == "check" }
            .configureEach { dependsOn(checkTask) }
    }
}
