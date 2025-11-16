plugins {
//    id("java") // 启用 Java 插件
    id("maven-publish") // 启用 Maven 发布插件 (Groovy 中的 'maven-publish')
    id("fabric-loom") // 启用 Fabric Loom 插件
    id("com.replaymod.preprocess") // 启用 ReplayMod 预处理器插件
    id("me.fallenbreath.yamlang") // 启用 yamlang 插件
}

val mcVersion: Int = project.property("mcVersion") as Int
val modId: String = project.property("mod_id") as String
val modName: String = project.property("mod_name") as String
val modVersion: String = project.property("mod_version") as String
val mavenGroup: String = project.property("maven_group") as String
val minecraftDependency: String = project.property("minecraft_dependency") as String
val minecraftVersion: String = project.property("minecraft_version") as String
val yarnMappings: String = project.property("yarn_mappings") as String
val loaderVersion: String = project.property("loader_version") as String
val fabricApiVersion: String = project.property("fabric_api_version") as String
val archivesBaseName: String = project.property("archives_base_name") as String

val mixinConfigPath = "blockminer.mixins.json"
val langDir = "assets/blockminer/lang"
val javaCompatibility = when {
    mcVersion >= 12005 -> JavaVersion.VERSION_21
    mcVersion >= 11800 -> JavaVersion.VERSION_17
    mcVersion >= 11700 -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val mixinCompatibilityLevel = javaCompatibility

repositories {
    maven("https://maven.fabricmc.net")
    maven("https://jitpack.io")
}

// https://github.com/FabricMC/fabric-loader/issues/783
configurations {
    named("modRuntimeOnly") {
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
}

loom {
    accessWidenerPath.set(file("../../src/main/resources/bedrockminer.accesswidener"))

    val commonVmArgs = listOf("-Dmixin.debug.export=true", "-Dmixin.debug.countInjections=true")
    runs.configureEach {
        // 确保它生成所有 "Minecraft Client (:subproject_name)" 应用程序
        runDir = "../../run"
        vmArgs(commonVmArgs)
    }

    //  // [功能] MIXIN 审计器 (MIXIN_AUDITOR)
    //  runs {
    //     val auditVmArgs = commonVmArgs + "-DmixinAuditor.audit=true"
    //     create("serverMixinAudit") {
    //        server() // 配置为运行服务器
    //        vmArgs(auditVmArgs)
    //        ideConfigGenerated.set(false) // 禁用 IDE 配置生成
    //     }
    //  }
}

// 示例版本值:
//   project.mod_version     1.0.3                      (基础 Mod 版本)
//   modVersionSuffix        +build.88                  (如果可能, 使用 GitHub Action 构建号)
//   artifactVersionSuffix   -SNAPSHOT
//   fullModVersion          1.0.3+build.88             (在 Mod 中使用的实际版本)
//   fullProjectVersion      v1.0.3-mc1.15.2+build.88   (在构建输出 jar 包名称中使用的版本)
//   fullArtifactVersion     1.0.3-mc1.15.2-SNAPSHOT    (Maven 产物版本)
var modVersionSuffix = ""
val artifactVersion = modVersion
var artifactVersionSuffix = ""
// 检测 GitHub Action 环境变量
// https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
if (System.getenv("BUILD_RELEASE") != "true") {
    val buildNumber = System.getenv("BUILD_ID")
    modVersionSuffix += if (buildNumber != null) "+build.$buildNumber" else "-SNAPSHOT"
    artifactVersionSuffix = "-SNAPSHOT"  // mapping-1.19-1.18.2.txt 的非发布版本产物始终是 SNAPSHOT 版本
}
val fullModVersion = modVersion + modVersionSuffix
var fullProjectVersion: String
var fullArtifactVersion: String
group = mavenGroup
if (System.getenv("JITPACK") == "true") {
    // 将 mc 版本移入 archivesBaseName 中，以便 Jitpack 能够正确组织来自多个子项目的产物
    base.archivesName.set(
        "$archivesBaseName-mc$minecraftVersion"
    )
    fullProjectVersion = "v$modVersion$modVersionSuffix"
    fullArtifactVersion = artifactVersion + artifactVersionSuffix
} else {
    base.archivesName.set(archivesBaseName)
    fullProjectVersion = "v$modVersion-mc$minecraftVersion$modVersionSuffix"
    fullArtifactVersion = "$artifactVersion-mc$minecraftVersion$artifactVersionSuffix"
}

version = fullProjectVersion

// 请参阅 https://youtrack.jetbrains.com/issue/IDEA-296490
// 如果 IDEA 抱怨 "Cannot resolve resource filtering of MatchingCopyAction"，并且你想知道原因
tasks.withType<ProcessResources> {
    inputs.property("id", modId)
    inputs.property("name", modName)
    inputs.property("version", fullModVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("minecraft_dependency", minecraftDependency)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "id" to modId,
                "name" to modName,
                "version" to fullModVersion,
                "loader_version" to loaderVersion,
                "minecraft_dependency" to minecraftDependency,
            )
        )
    }
    filesMatching(mixinConfigPath) {
        filter { it.replace("{{COMPATIBILITY_LEVEL}}", "JAVA_${mixinCompatibilityLevel.ordinal + 1}") }
    }
}

// https://github.com/Fallen-Breath/yamlang
yamlang {
    targetSourceSets.set(setOf(sourceSets.main.get()))
    inputDir.set(langDir)
}

// 确保编码设置为 UTF-8，无论系统默认值是什么
// 这修复了某些特殊字符无法正确显示的边缘情况
// 参阅 http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    if (javaCompatibility <= JavaVersion.VERSION_1_8) {
        // 压制 "source/target value 8 is obsolete and will be removed in a future release" 的警告
        options.compilerArgs.add("-Xlint:-options")
    }
}

java {
    sourceCompatibility = javaCompatibility
    targetCompatibility = javaCompatibility

    // 如果存在，Loom 会自动将 sourcesJar 附加到 RemapSourcesJar 任务和 "build" 任务
    // 如果移除此行，则不会生成源码。
    withSourcesJar()
}

tasks.withType<Jar> {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${archivesBaseName}" }
    }
}

// 配置 Maven 发布
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesName.get()
            version = fullArtifactVersion
        }
    }

    // 选择要发布到的仓库
    repositories {
        // 本地 Maven 仓库
        // mavenLocal()

//     // [功能] FALLENS_MAVEN 仓库
//     maven {
//        // 如果是 SNAPSHOT 版本，发布到快照仓库；否则发布到发布仓库
//        url = uri(if (fullArtifactVersion.endsWith("SNAPSHOT")) "https://maven.fallenbreath.me/snapshots" else "https://maven.fallenbreath.me/releases")
//        credentials {
//           username = "fallen"
//           // 从环境变量获取密码/令牌
//           password = System.getenv("FALLENS_MAVEN_TOKEN")
//        }
//        authentication {
//           // 使用 Basic 认证
//           create<BasicAuthentication>("basic")
//        }
//     }
    }
}