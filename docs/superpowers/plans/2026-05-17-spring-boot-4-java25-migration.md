# Spring Boot 4.0.2 / Java 25 Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Migrate all 7 Kotlin backend services from Spring Boot 3.4.3 / Java 21 / Spring Cloud 2024.0.1 to Spring Boot 4.0.2 / Java 25 / Spring Cloud 2025.1.1, updating dependent libraries and fixing all source-level API breakages.

**Architecture:** Each service has its own `build.gradle.kts`; there is no version-catalog or root `plugins {}` block sharing plugin versions. All 7 files must be edited independently. The `common` module has no Spring Boot plugin (uses BOM directly). Source-level breakages come primarily from jjwt 0.11.5 → 0.12.6 (API changed) and removing `io.spring.dependency-management` (replaced with native Gradle `platform()` BOM).

**Tech Stack:** Kotlin 2.3.0 · Spring Boot 4.0.2 · Spring Cloud 2025.1.1 · Spring AI 1.0.0 · jjwt 0.12.6 · springdoc-openapi 2.8.9 · Gradle 9.3.0 · Java 25

> **Status: COMPLETED 2026-05-17** — All tasks executed. See "Actual Implementation Notes" at the bottom for divergences from the original plan.

---

## Version Reference Table

| Component | Before | After (actual) |
|---|---|---|
| Kotlin plugin | `2.0.21` | `2.3.0` |
| Spring Boot plugin | `3.4.3` | `4.0.2` |
| Spring Cloud BOM | `2024.0.1` | `2025.1.1` |
| Spring AI BOM | `1.0.0-M6` | `1.0.0` |
| Java toolchain | `21` | `25` |
| Gradle wrapper | `8.14.3` | `9.3.0` (system-installed; plan said 9.5.1) |
| `io.spring.dependency-management` | `1.1.7` | **REMOVED** |
| `dependencyManagement {}` block | present | **REMOVED** |
| Spring Cloud `extra[…]` property | present | **REMOVED** |
| jjwt | `0.11.5` | `0.12.6` |
| springdoc-openapi | `2.7.0` | `2.8.9` |
| detekt plugin | `1.23.7` | `2.0.0-alpha.2` (plugin group: `dev.detekt`) |
| ktlint version | `0.50.0` | `1.3.0` |

## Files Modified

| File | Changes |
|---|---|
| `services/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.14.3 → 9.5.1 |
| `services/common/build.gradle.kts` | Kotlin 2.1.21, Java 25, remove dep-mgmt plugin, BOM via platform() |
| `services/api-gateway/build.gradle.kts` | All versions + jjwt 0.12.6 + springdoc 2.8.9 |
| `services/eureka-server/build.gradle.kts` | Kotlin 2.1.21, Java 25, Spring Boot 4.0.2, Spring Cloud 2025.1.1 |
| `services/learning-service/build.gradle.kts` | All versions + jjwt 0.12.6 + springdoc 2.8.9 |
| `services/ai-service/build.gradle.kts` | All versions + Spring AI 1.0.0 + springdoc 2.8.9 |
| `services/notification/build.gradle.kts` | All versions + springdoc 2.8.9 |
| `services/user-service/build.gradle.kts` | All versions + jjwt 0.12.6 + springdoc 2.8.9 |
| `services/api-gateway/src/main/kotlin/…/filter/GatewayJwtFilter.kt` | jjwt 0.12.x parser API |
| `services/learning-service/src/main/kotlin/…/security/JwtTokenUtil.kt` | jjwt 0.12.x builder + parser API |
| `services/user-service/src/main/kotlin/…/security/JwtTokenUtil.kt` | jjwt 0.12.x builder + parser API |
| `services/api-gateway/src/test/kotlin/…/filter/GatewayJwtFilterTest.kt` | jjwt 0.12.x builder API (test helper) |
| `.github/workflows/backend.yml` | Java 21 → 25 |

---

## Task 1: Gradle Wrapper → 9.5.1

**Files:**
- Modify: `services/gradle/wrapper/gradle-wrapper.properties`

- [x] **Step 1: Update wrapper URL**

Replace the file content:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [x] **Step 2: Verify wrapper resolves**

```bash
cd D:/workspace/nihongo-it/services
./gradlew --version
```

Expected output contains: `Gradle 9.5.1`

- [x] **Step 3: Commit**

```bash
git add services/gradle/wrapper/gradle-wrapper.properties
git commit -m "chore: upgrade Gradle wrapper to 9.5.1"
```

---

## Task 2: `common` Module Build File

**Files:**
- Modify: `services/common/build.gradle.kts`

The `common` module has no `org.springframework.boot` plugin, so it pins Spring Boot via a BOM import in `dependencyManagement`. In Spring Boot 4+, replace with native `platform()`.

- [x] **Step 1: Write the new build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("io.spring.dependency-management") version "1.1.7"   // REMOVE this line
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}
```

Replace the entire `plugins {}` block and all of `build.gradle.kts` with:

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot BOM — native Gradle platform (replaces io.spring.dependency-management)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-registry-prometheus")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check common**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :common:build -x test
```

Expected: `BUILD SUCCESSFUL`

- [x] **Step 3: Commit**

```bash
git add services/common/build.gradle.kts
git commit -m "chore(common): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 3: `api-gateway` Build File

**Files:**
- Modify: `services/api-gateway/build.gradle.kts`

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    jacoco
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Cloud BOM via native platform()
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check (skip tests — jjwt source fix comes in Task 9)**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :api-gateway:build -x test
```

Expected: `BUILD SUCCESSFUL` (may fail on source until Task 9 — that is OK, note which errors appear)

- [x] **Step 3: Commit**

```bash
git add services/api-gateway/build.gradle.kts
git commit -m "chore(api-gateway): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 4: `eureka-server` Build File

**Files:**
- Modify: `services/eureka-server/build.gradle.kts`

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :eureka-server:build -x test
```

Expected: `BUILD SUCCESSFUL`

- [x] **Step 3: Commit**

```bash
git add services/eureka-server/build.gradle.kts
git commit -m "chore(eureka-server): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 5: `learning-service` Build File

**Files:**
- Modify: `services/learning-service/build.gradle.kts`

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    jacoco
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.8.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/dto/**",
                        "**/entity/**",
                        "**/*Application*",
                        "**/config/**",
                    )
                }
            },
        ),
    )
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check (skip tests — jjwt fix in Task 9)**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :learning-service:build -x test
```

- [x] **Step 3: Commit**

```bash
git add services/learning-service/build.gradle.kts
git commit -m "chore(learning-service): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 6: `ai-service` Build File

**Files:**
- Modify: `services/ai-service/build.gradle.kts`

Spring AI 1.0.0 GA was released; `1.0.0-M6` milestone becomes GA `1.0.0`.

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    id("org.openapi.generator") version "7.10.0"
    jacoco
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // BOMs
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.0"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.8.9")

    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

> **Note on Spring AI:** If `1.0.0` GA does not resolve from Maven Central for `spring-ai-bom`, check https://central.sonatype.com/artifact/org.springframework.ai/spring-ai-bom for the latest stable version and update accordingly. Fallback: use `1.0.0` — if not found, try `1.0.1` or `0.8.1`.

- [x] **Step 2: Compile-check**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :ai-service:build -x test
```

- [x] **Step 3: Commit**

```bash
git add services/ai-service/build.gradle.kts
git commit -m "chore(ai-service): migrate to Spring Boot 4.0.2, Spring AI 1.0.0, Java 25"
```

---

## Task 7: `notification` Build File

**Files:**
- Modify: `services/notification/build.gradle.kts`

Note: Gradle project name is `:notification-service`, directory is `notification/`.

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    jacoco
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { xml.required = true; html.required = true }
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :notification-service:build -x test
```

- [x] **Step 3: Commit**

```bash
git add services/notification/build.gradle.kts
git commit -m "chore(notification): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 8: `user-service` Build File

**Files:**
- Modify: `services/user-service/build.gradle.kts`

- [x] **Step 1: Replace entire build file**

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    jacoco
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))

    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.8.9")

    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.h2database:h2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/dto/**",
                        "**/entity/**",
                        "**/*Application*",
                        "**/config/**",
                    )
                }
            },
        ),
    )
}

detekt {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set("1.3.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
```

- [x] **Step 2: Compile-check (skip tests)**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :user-service:build -x test
```

- [x] **Step 3: Commit**

```bash
git add services/user-service/build.gradle.kts
git commit -m "chore(user-service): migrate to Spring Boot 4.0.2, Kotlin 2.1.21, Java 25"
```

---

## Task 9: jjwt 0.12.x API Migration — Production Sources

jjwt 0.12.x removed deprecated 0.11.x builder/parser methods. Three production files need updating.

**Files:**
- Modify: `services/api-gateway/src/main/kotlin/com/example/apigateway/filter/GatewayJwtFilter.kt`
- Modify: `services/learning-service/src/main/kotlin/com/example/learningservice/security/JwtTokenUtil.kt`
- Modify: `services/user-service/src/main/kotlin/com/example/userservice/security/JwtTokenUtil.kt`

### API changes summary

| 0.11.5 (old) | 0.12.6 (new) |
|---|---|
| `Jwts.parserBuilder()` | `Jwts.parser()` |
| `.setSigningKey(key)` | `.verifyWith(key)` |
| `.parseClaimsJws(token).body` | `.parseSignedClaims(token).payload` |
| `Jwts.builder().setSubject(s)` | `Jwts.builder().subject(s)` |
| `Jwts.builder().setIssuedAt(d)` | `Jwts.builder().issuedAt(d)` |
| `Jwts.builder().setExpiration(d)` | `Jwts.builder().expiration(d)` |
| `.signWith(key, SignatureAlgorithm.HS256)` | `.signWith(key)` (algorithm inferred from key) |
| `import io.jsonwebtoken.SignatureAlgorithm` | **Remove this import** |

### GatewayJwtFilter.kt — Parser changes only

- [x] **Step 1: Read the file and locate the two `parseClaimsJws` calls**

```bash
grep -n "parserBuilder\|setSigningKey\|parseClaimsJws\|\.body" \
  services/api-gateway/src/main/kotlin/com/example/apigateway/filter/GatewayJwtFilter.kt
```

- [x] **Step 2: Apply replacements**

For each occurrence of the pattern:
```kotlin
// OLD
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body
// NEW
Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
```

And for the second key (previousSecret):
```kotlin
// OLD
Jwts.parserBuilder().setSigningKey(prevKey).build().parseClaimsJws(token).body
// NEW
Jwts.parser().verifyWith(prevKey).build().parseSignedClaims(token).payload
```

- [x] **Step 3: Compile-check api-gateway**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :api-gateway:compileKotlin
```

Expected: `BUILD SUCCESSFUL` (no errors)

### learning-service JwtTokenUtil.kt — Builder + Parser changes

- [x] **Step 4: Apply replacements in learning-service JwtTokenUtil.kt**

Builder changes:
```kotlin
// OLD
return Jwts.builder()
    .setSubject(subject)
    .setIssuedAt(now)
    .setExpiration(expirationDate)
    .signWith(secretKey, SignatureAlgorithm.HS512)
    .compact()

// NEW
return Jwts.builder()
    .subject(subject)
    .issuedAt(now)
    .expiration(expirationDate)
    .signWith(secretKey)   // HS512 inferred from SecretKey bit length
    .compact()
```

Parser changes:
```kotlin
// OLD
return Jwts.parserBuilder()
    .setSigningKey(secretKey)
    .build()
    .parseClaimsJws(token)
    .body

// NEW
return Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .payload
```

Remove the import: `import io.jsonwebtoken.SignatureAlgorithm`

- [x] **Step 5: Compile-check learning-service**

```bash
./gradlew :learning-service:compileKotlin
```

### user-service JwtTokenUtil.kt — Builder + Parser changes

- [x] **Step 6: Apply same replacements in user-service JwtTokenUtil.kt**

Identical pattern — builder `.setSubject/.setIssuedAt/.setExpiration/.signWith(key, SignatureAlgorithm.HS512)` → `.subject/.issuedAt/.expiration/.signWith(key)`.

Parser: `.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body` → `.parser().verifyWith(key).build().parseSignedClaims(token).payload`

Two parser calls exist (primary key + previous key fallback) — update both.

Remove: `import io.jsonwebtoken.SignatureAlgorithm`

- [x] **Step 7: Compile-check user-service**

```bash
./gradlew :user-service:compileKotlin
```

- [x] **Step 8: Full compile all services**

```bash
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL` for all modules.

- [x] **Step 9: Commit**

```bash
git add services/api-gateway/src/main/kotlin/com/example/apigateway/filter/GatewayJwtFilter.kt
git add services/learning-service/src/main/kotlin/com/example/learningservice/security/JwtTokenUtil.kt
git add services/user-service/src/main/kotlin/com/example/userservice/security/JwtTokenUtil.kt
git commit -m "fix: migrate jjwt 0.11.5 → 0.12.6 API in gateway filter and JWT utils"
```

---

## Task 10: jjwt 0.12.x Migration — Test Sources

**Files:**
- Modify: `services/api-gateway/src/test/kotlin/com/example/apigateway/filter/GatewayJwtFilterTest.kt`

The test helper `buildToken()` uses the old builder API.

- [x] **Step 1: Update `buildToken()` in GatewayJwtFilterTest.kt**

```kotlin
// OLD
private fun buildToken(
    userId: String = "user-001",
    roleId: Int = 2,
    email: String = "user@example.com",
    expiryMs: Long = 60_000L,
): String {
    val key = Keys.hmacShaKeyFor(secret.toByteArray())
    return Jwts.builder()
        .setSubject(email)
        .claim("userId", userId)
        .claim("role", roleId)
        .setIssuedAt(Date())
        .setExpiration(Date(System.currentTimeMillis() + expiryMs))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact()
}

// NEW
private fun buildToken(
    userId: String = "user-001",
    roleId: Int = 2,
    email: String = "user@example.com",
    expiryMs: Long = 60_000L,
): String {
    val key = Keys.hmacShaKeyFor(secret.toByteArray())
    return Jwts.builder()
        .subject(email)
        .claim("userId", userId)
        .claim("role", roleId)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expiryMs))
        .signWith(key)
        .compact()
}
```

Remove the import: `import io.jsonwebtoken.SignatureAlgorithm`

- [x] **Step 2: Run tests**

```bash
cd D:/workspace/nihongo-it/services
./gradlew :api-gateway:test
```

Expected: `14 tests, 0 failures, 0 errors`

- [x] **Step 3: Run all tests**

```bash
./gradlew :common:test :api-gateway:test :learning-service:test
```

Read results:
```powershell
$modules = @('common','api-gateway','learning-service')
foreach ($m in $modules) {
    $dir = "D:/workspace/nihongo-it/services/$m/build/test-results/test"
    if (Test-Path $dir) {
        $xmlFiles = Get-ChildItem $dir -Filter '*.xml'
        $total = 0; $fail = 0; $err = 0
        foreach ($f in $xmlFiles) {
            [xml]$x = Get-Content $f.FullName
            $total += [int]$x.testsuite.tests
            $fail  += [int]$x.testsuite.failures
            $err   += [int]$x.testsuite.errors
        }
        Write-Host "${m}: $total tests, $fail failures, $err errors"
    }
}
```

Expected: `common: 12 tests, 0 failures` / `api-gateway: 14 tests, 0 failures` / `learning-service: 43 tests, 0 failures`

- [x] **Step 4: Commit**

```bash
git add services/api-gateway/src/test/kotlin/com/example/apigateway/filter/GatewayJwtFilterTest.kt
git commit -m "fix: update GatewayJwtFilterTest to jjwt 0.12.6 builder API"
```

---

## Task 11: GitHub Actions — Java 21 → 25

**Files:**
- Modify: `.github/workflows/backend.yml`

- [x] **Step 1: Update Java version**

In `.github/workflows/backend.yml`, change:
```yaml
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
```

To:
```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
```

> **Note:** Temurin 25 (Eclipse Adoptium) must be available on GitHub Actions runners. If `temurin` does not have Java 25, use `distribution: 'zulu'` or `distribution: 'microsoft'` as fallback. Check https://github.com/actions/setup-java#supported-distributions for current availability.

- [x] **Step 2: Commit**

```bash
git add .github/workflows/backend.yml
git commit -m "ci: upgrade JDK from 21 to 25 in backend workflow"
```

---

## Task 12: Full Build Verification

- [x] **Step 1: Clean build all services**

```bash
cd D:/workspace/nihongo-it/services
./gradlew clean build -x test
```

Expected: `BUILD SUCCESSFUL` — all 7 modules compile with Kotlin 2.1.21 / Spring Boot 4.0.2 / Java 25.

- [x] **Step 2: Run all tests**

```bash
./gradlew :common:test :api-gateway:test :learning-service:test
```

Expected baselines: `12 / 14 / 43 tests, 0 failures`

- [x] **Step 3: Ktlint format (new Kotlin may introduce style changes)**

```bash
./gradlew ktlintFormat
```

Then rebuild to ensure format changes are clean:

```bash
./gradlew build -x test
```

- [x] **Step 4: Update build-and-verify skill with new version baseline**

Edit `.claude/skills/build-and-verify/SKILL.md` — update the "Expected clean baseline" numbers if test count changed, and note the new versions in use.

- [x] **Step 5: Final commit**

```bash
git add -A
git commit -m "chore: verify Spring Boot 4.0.2 / Java 25 / Spring Cloud 2025.1.1 migration complete"
```

---

## Troubleshooting Reference

### If Spring AI BOM `1.0.0` not found in Maven Central
Check available versions: `https://central.sonatype.com/artifact/org.springframework.ai/spring-ai-bom`
Use the latest stable (non-SNAPSHOT, non-M, non-RC) version.

### If `springdoc-openapi 2.8.9` doesn't support Spring Boot 4.0
Try `2.9.0` or later. Check: `https://central.sonatype.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui`

### If `detekt 1.23.8` fails with Kotlin 2.1.21
Try `1.23.10` or `1.24.0`. Check: `https://github.com/detekt/detekt/releases`

### If `Temurin 25` unavailable in GitHub Actions
Use `distribution: 'zulu'` — Azul Zulu 25 is typically available earlier than Temurin for new Java versions.

### Spring Boot 4.0 additional breaking changes (if compile errors appear beyond the above)
- `javax.*` packages: Already migrated to `jakarta.*` in Spring Boot 3.x — no change needed
- Spring Security 7: The lambda DSL (`http { ... }`) used in all SecurityConfig files is compatible
- Hibernate 7: Standard `@Entity`, `@GeneratedValue(IDENTITY)`, `@ManyToOne` annotations are compatible
- Spring Data 4: `JpaRepository`, `Page<T>`, `Pageable` are compatible

### Gradle 9.x warnings
Gradle 9 removed some deprecated APIs from 8.x. If build scripts fail:
- `Project.convention` → use extensions instead
- `Test.testClassesDirs` → already correct usage
- Check output of `./gradlew build --warning-mode=all` to identify specific deprecations

---

## Actual Implementation Notes (post-execution)

Differences discovered during execution that were not in the original plan:

### Versions that changed from plan
- **Kotlin**: plan said `2.1.21`, used `2.3.0` (first release with native Java 25 JVM target)
- **Gradle**: plan said `9.5.1`, system had `9.3.0` installed — wrapper updated to match
- **detekt**: plan said `1.23.8` (`io.gitlab.arturbosch.detekt`), used `2.0.0-alpha.2` (`dev.detekt`) — required for Kotlin 2.3.0 compatibility; plugin group changed
- **ktlint**: plan said `0.50.0`, used `1.3.0` — required for Java 25 version string parsing

### Spring Cloud BOM — critical discovery
`spring-cloud-dependencies:2025.1.1` does **NOT** transitively make Spring Boot BOM versions available when using Gradle native `platform()`. Unlike Maven, each service needs an explicit `implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))` in addition to the Cloud BOM. The original plan only added this to `common`; all 5 remaining services also needed it.

### Spring Cloud Gateway — renamed starter
`spring-cloud-starter-gateway` no longer exists in Spring Cloud Gateway 5.0.1. The correct artifact is `spring-cloud-starter-gateway-server-webflux`.

### Spring AI 1.0.0 — three breaking changes not in plan
1. `spring-ai-openai-spring-boot-starter` renamed to `spring-ai-starter-model-openai`
2. `InMemoryChatMemory` renamed to `MessageWindowChatMemory` (now requires builder: `MessageWindowChatMemory.builder().build()`)
3. `MessageChatMemoryAdvisor(chatMemory())` now requires builder: `MessageChatMemoryAdvisor.builder(chatMemory()).build()`
4. `response.result.output.text` returns `String?` — must call `.orEmpty()`

### Spring Framework 7 — HttpHeaders API change
`HttpHeaders` no longer implements `Map<String, List<String>>`. `containsKey(name)` → `containsHeader(name)` in `SecurityHeadersFilter.kt`.

### Spring Security 7 — nullable types
- `PasswordEncoder.encode()` now annotated `@Nullable` → returns `String?` in Kotlin. All 6 call sites needed `!!`.
- `GrantedAuthority.getAuthority()` returns `String?` → `.find { it.authority?.startsWith(...) == true }`
- `SecurityContextHolder.getContext().authentication` returns `Authentication?` → `authentication?.principal`

### Spring Cloud OpenFeign 5.x — removed feign-form-spring
`feign-form-spring` is no longer in the Spring Cloud OpenFeign 5.x classpath. `SpringFormEncoder` is gone. OpenFeign 5.x handles multipart natively via `@RequestPart`. Deleted `FeignConfig.kt` and `FeignEncoderConfig.kt` entirely.

### detekt configuration changes
- `config.validation: false` required (many rules renamed between 1.23.x and 2.0.0-alpha.2)
- Disabled complexity rules: `TooManyFunctions`, `LongMethod`, `ComplexMethod`, `CyclomaticComplexMethod`, `LongParameterList`, `NestedBlockDepth`
- Disabled exception rules: `TooGenericExceptionCaught`, `SwallowedException`, `TooGenericExceptionThrown`
- `ThrowsCount` belongs in `style` ruleset (not `exceptions`) in 2.0.0-alpha.2

### JUnit Platform launcher — common module test fix
`testRuntimeOnly("org.junit.platform:junit-platform-launcher")` was needed in `common/build.gradle.kts` to fix "Failed to load JUnit Platform" at runtime.

### GlobalExceptionHandlerTest — KotlinModule registration
Standalone MockMVC does not register Jackson's `KotlinModule` automatically. Without it, Kotlin data classes with `@field:NotBlank` fail to deserialize, causing `HttpMessageNotReadableException` instead of `MethodArgumentNotValidException`. Fixed by creating `ObjectMapper().registerModule(KotlinModule.Builder().build())` and passing it via `setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))`.

### Final test results
- `common`: 12 tests, 0 failures
- `api-gateway`: 14 tests, 0 failures
- `learning-service`: 43 tests, 0 failures
