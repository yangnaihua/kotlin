@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*


var Project.bootstrapKotlinVersion: String
    get() = this.property("bootstrapKotlinVersion") as String
    private set(value) { this.extra["bootstrapKotlinVersion"] = value }
var Project.bootstrapKotlinRepo: String?
    get() = this.property("bootstrapKotlinRepo") as String?
    private set(value) { this.extra["bootstrapKotlinRepo"] = value }

fun Project.kotlinBootstrapFrom(defaultSource: BootstrapOption) {
    val bootstrapVersion = project.findProperty("bootstrap.kotlin.version") as String?
    val bootstrapRepo = project.findProperty("bootstrap.kotlin.repo") as String?
    val bootstrapTeamCityVersion = project.findProperty("bootstrap.teamcity.kotlin.version") as String?

    val bootstrapSource = when {
        bootstrapVersion != null -> BootstrapOption.Custom(kotlinVersion = bootstrapVersion, repo = bootstrapRepo)
        bootstrapTeamCityVersion != null -> BootstrapOption.TeamCity(bootstrapTeamCityVersion, onlySuccessBootstrap = false)
        project.hasProperty("bootstrap.local") -> BootstrapOption.Local(project.findProperty("bootstrap.local.version") as String?)
        else -> defaultSource
    }

    bootstrapSource.applyToProject(project)
    project.logger.lifecycle("Using kotlin bootstrap version $bootstrapKotlinVersion from repo $bootstrapKotlinRepo")
}

sealed class BootstrapOption {
    abstract fun applyToProject(project: Project)

    /** Manual repository and version specification.
     *
     *  If [repo] is not specified the default buildscript and project repositories are used
     */
    class Custom(val kotlinVersion: String, val repo: String?) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            project.bootstrapKotlinVersion = kotlinVersion
            project.bootstrapKotlinRepo = repo
        }
    }
    /** Get bootstrap from kotlin-dev bintray repo, where bootstraps are published */
    class BintrayDev(val kotlinVersion: String) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            project.bootstrapKotlinVersion = kotlinVersion
            project.bootstrapKotlinRepo = "https://dl.bintray.com/kotlin/kotlin-dev"
        }
    }
    /** Get bootstrap from teamcity maven artifacts of the specified build configuration
     *
     * [kotlinVersion] build number and the version of maven artifacts
     * [projectExtId] extId of a teamcity build configuration, by default "Kotlin_dev_Compiler",
     * [onlySuccessBootstrap] allow artifacts only from success builds of the default branch tagged with 'bootstrap' tag
     */
    class TeamCity(val kotlinVersion: String, val projectExtId: String? = null, val onlySuccessBootstrap: Boolean = true) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            val query = if (onlySuccessBootstrap) "status:SUCCESS,tag:bootstrap,pinned:true" else "branch:default:any"
            project.bootstrapKotlinRepo = "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:${projectExtId ?: "Kotlin_dev_Compiler"}),number:$kotlinVersion,$query/artifacts/content/maven/"
            project.bootstrapKotlinVersion = kotlinVersion
        }
    }

    /**
     * Use previously published local artifacts from the build/repo maven repository
     */
    class Local(val kotlinVersion: String? = null) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            project.bootstrapKotlinRepo = project.buildDir.resolve("repo").toURI().toString()
            project.bootstrapKotlinVersion = kotlinVersion ?: project.property("defaultSnapshotVersion") as String
        }
    }
}