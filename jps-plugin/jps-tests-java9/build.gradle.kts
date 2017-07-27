apply { plugin("kotlin") }

javaHome = rootProject.extra["JDK_9"] as String

dependencies {
    testCompile(project(":jps-plugin"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(project(":compiler.tests-common"))
    testCompile(projectTests(":compiler.tests-common"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(ideaSdkDeps("jps-builders", "jps-builders-6", subdir = "jps"))
    testCompileOnly(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps/test"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps"))
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
    }
}

projectTest {
    workingDir = rootDir
}

testsJar {}
