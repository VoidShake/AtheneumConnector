
import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import net.minecraftforge.gradle.userdev.jarjar.JarJarProjectExtension

val mod_id: String by extra
val mod_version: String by extra
val dynmap_version: String by extra
val rabbitmq_version: String by extra
val travelers_titles_version: String by extra
val yungs_api_version: String by extra
val graphql_client_version: String by extra
val ktor_client_version: String by extra
val kotlin_forge_version: String by extra
val atheneum_version: String by extra

plugins {
    idea
    id("com.possible-triangle.gradle") version ("0.2.5")
    id("com.expediagroup.graphql") version ("8.1.0")
}

withKotlin()

forge {
    includesLibrary("com.rabbitmq:amqp-client:$rabbitmq_version")
    includesLibrary("com.expediagroup:graphql-kotlin-ktor-client:$graphql_client_version")
    includesLibrary("com.possible-triangle:atheneum-models:$atheneum_version")

    kotlinForgeVersion = null
}

repositories {
    modrinthMaven()
    localMaven(project)

    maven {
        url = uri("https://maven.pkg.github.com/VoidShake/Atheneum")
        credentials {
            username = env["GITHUB_ACTOR"]
            password = env["GITHUB_TOKEN"]
        }
        content {
            includeGroup("com.possible-triangle")
        }
    }
}

// required because of duplicate package export
configurations.named("minecraftLibrary") {
    exclude(group = "org.jetbrains", module = "annotations")
}

val jarJar = the<JarJarProjectExtension>()

dependencies {
    modImplementation("maven.modrinth:travelers-titles:${travelers_titles_version}")
    modRuntimeOnly("maven.modrinth:yungs-api:${yungs_api_version}")
    modRuntimeOnly("maven.modrinth:dynmap:${dynmap_version}")

    // required because of duplicate package export by thedarkcolour:kotlinforforge:all
    implementation("thedarkcolour:kffmod:${kotlin_forge_version}")
    implementation("thedarkcolour:kfflang:${kotlin_forge_version}")
    implementation("thedarkcolour:kfflib:${kotlin_forge_version}")

    fun pin(dependency: String) {
        add("jarJar", dependency) {
            jarJar.ranged(this, "[${version},)")
        }
    }

    pin("com.expediagroup:graphql-kotlin-client:$graphql_client_version")
    pin("com.expediagroup:graphql-kotlin-client-serialization:$graphql_client_version")
    pin("com.expediagroup:graphql-kotlin-client-serialization:$graphql_client_version")
    pin("io.ktor:ktor-utils-jvm:$ktor_client_version")
    pin("io.ktor:ktor-client-core-jvm:$ktor_client_version")
    pin("io.ktor:ktor-client-cio-jvm:$ktor_client_version")
    pin("io.ktor:ktor-client-serialization-jvm:$ktor_client_version")
    pin("io.ktor:ktor-network-jvm:$ktor_client_version")
    pin("io.ktor:ktor-network-tls-jvm:$ktor_client_version")
    pin("io.ktor:ktor-events-jvm:$ktor_client_version")
    pin("io.ktor:ktor-utils-jvm:$ktor_client_version")
    pin("io.ktor:ktor-io-jvm:$ktor_client_version")
    pin("io.ktor:ktor-http-jvm:$ktor_client_version")
    pin("io.ktor:ktor-http-cio-jvm:$ktor_client_version")

    //add("minecraftLibrary", "org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
}

graphql {
    client {
        endpoint = env["GRAPHQL_ENDPOINT"] ?: "https://atlas.macarena.ceo/api/graphql"
        packageName = "com.possible_triangle.atheneum_connector.generated"
        serializer = GraphQLSerializer.KOTLINX
    }
}

tasks.withType<Jar> {
    exclude("queries")
}

enablePublishing {
    githubPackages()
}

uploadToModrinth {
    syncBodyFromReadme()
}

val copy = task<Copy>("copyBuild") {
    group = "build"

    from("build/libs/${mod_id}-${mod_version}.jar")
    into("docker/mods")

    dependsOn("jarJar")
}

tasks.build {
    dependsOn(copy)
}

enableSonarQube()
