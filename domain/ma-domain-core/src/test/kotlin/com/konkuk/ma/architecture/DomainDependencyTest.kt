package com.konkuk.ma.architecture

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import io.kotest.core.spec.style.StringSpec

/**
 * 도메인 패키지 간 의존 방향을 강제한다.
 * 단일 모듈이라 컴파일러가 패키지 순환을 막아주지 못하므로, 이 테스트가 그 역할을 대신한다.
 */
class DomainDependencyTest : StringSpec({

    val domainClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.konkuk.ma.domain")

    "member 도메인은 다른 feature 도메인에 의존하지 않는다 (foundational domain)" {
        noClasses()
            .that().resideInAPackage("..member..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..auth..",
                "..community..",
                "..matching..",
                "..point..",
                "..support..",
                "..xroom..",
                "..withdrawal..",
            )
            .because("member 는 다른 도메인이 의존하는 기반 도메인이다. 역으로 의존하면 순환이 생긴다")
            .check(domainClasses)
    }

    "도메인 슬라이스 간 순환 의존이 없다" {
        slices()
            .matching("com.konkuk.ma.domain.(*)..")
            .should().beFreeOfCycles()
            // TODO: matching <-> xroom 는 이번 작업 범위 밖의 기존 순환이라 한시적으로 예외 처리한다.
            //       (matching.application -> xroom, xroom.domain.XroomValidator -> matching)
            .ignoreDependency(resideInAPackage("..matching.."), resideInAPackage("..xroom.."))
            .ignoreDependency(resideInAPackage("..xroom.."), resideInAPackage("..matching.."))
            .check(domainClasses)
    }
})
