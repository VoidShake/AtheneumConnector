import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql

val mc_version: String by extra
val rabbitmq_version: String by extra
val travelers_titles_version: String by extra
val yungs_api_version: String by extra
val graphql_client_version: String by extra
val kotlin_forge_version: String by extra

plugins {
    idea
    id("com.possible-triangle.gradle") version ("0.2.5")
    id("com.expediagroup.graphql") version ("8.1.0")
}

withKotlin()

forge {
    includesLibrary("com.rabbitmq:amqp-client:$rabbitmq_version")
    includesLibrary("com.expediagroup:graphql-kotlin-ktor-client:$graphql_client_version")

    kotlinForgeVersion = null
}

repositories {
    modrinthMaven()
}

// required because of duplicate package export
configurations.named("minecraftLibrary") {
    exclude(group = "org.jetbrains", module = "annotations")
}

dependencies {
    modImplementation("maven.modrinth:travelers-titles:${travelers_titles_version}")
    modRuntimeOnly("maven.modrinth:yungs-api:${yungs_api_version}")

    // required because of duplicate package export by thedarkcolour:kotlinforforge:all
    implementation("thedarkcolour:kffmod:${kotlin_forge_version}")
    implementation("thedarkcolour:kfflang:${kotlin_forge_version}")
    implementation("thedarkcolour:kfflib:${kotlin_forge_version}")

    //add("minecraftLibrary", "org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")

    /*
    "minecraftLibrary"("com.expediagroup:graphql-kotlin-ktor-client:$graphql_client_version") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains")
    }

    "jarJar"("com.expediagroup:graphql-kotlin-ktor-client:$graphql_client_version") {
        jarJar.pin(this, "[${graphql_client_version},)")
    }
     */
}

graphql {
    client {
        endpoint = env["GRAPHQL_ENDPOINT"] ?: "https://atlas.dev.macarena.ceo/api/graphql"
        packageName = "com.possible_triangle.atheneum_connector.generated"
        serializer = GraphQLSerializer.KOTLINX
    }
}

enablePublishing {
    githubPackages()
}

uploadToModrinth {
    syncBodyFromReadme()
}

enableSonarQube()
