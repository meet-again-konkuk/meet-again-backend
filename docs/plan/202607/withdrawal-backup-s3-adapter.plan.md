# Plan: 탈퇴 회원 데이터 백업 S3 어댑터 (`S3MemberBackupStorage`)

> 작성일: 2026-07-17 · 상태: ✅ 구현 완료 (2026-07-17)
> 브랜치: `feat/withdrawal-backup-s3-adapter`

## ✅ 구현 완료 — plan 대비 달라진 점

- **`s3Presigner` 빈 분리를 이번 PR에 반영** (plan §8에선 선택적 후속): code-reviewer가 "조건과 실제 사용처 불일치"로 지적 → `@Bean` 메서드에 `@ConditionalOnProperty(file.storage.mode=s3)` 추가. 백업-only-s3에서 presigner가 더는 뜨지 않는다.
- **(중대) httpclient5/httpcore5 버전 충돌 발견·수정**: AWS SDK 2.46의 `apache5-client`는 httpclient5 5.4+/httpcore5 5.4+를 요구하는데 Spring Boot 3.3.4 BOM이 5.3.1/5.2.5로 다운그레이드 → **s3 모드에서 `S3Client` 빈 생성 자체가 `NoClassDefFoundError`(TlsSocketStrategy)로 부팅 실패**. 기존 사진 S3 경로에도 잠복해 있던 버그(어떤 테스트도 S3Client를 실생성하지 않아 미검출). → 루트 `build.gradle.kts`에 `ext["httpclient5.version"]="5.6.1"`, `ext["httpcore5.version"]="5.4.3"` 오버라이드로 수정.
- **테스트가 plan(결정 5: mockk 단위만)보다 확장됨**: 위 충돌과 문자열 SpEL 조건은 단위 테스트로 안 잡혀서, 순수 Spring 컨텍스트(`AnnotationConfigApplicationContext`) 기반 `MemberBackupStorageWiringTest` 추가 — 모드 조합 4종의 빈 활성화 + S3Client 실생성을 고정. ⚠️ 이 레포에 선례 없는 테스트 패턴(스킬 §0)이라 유지 여부는 PR 리뷰에서 판단.
- `file.s3.region` 공유 제약(백업-only-s3여도 필수)은 개명 대신 주석 1줄로 명시 (리뷰 지적 반영, 개명은 §8 후속 유지).
> 선행: `docs/plan/202606/withdrawal-data-backup.plan.md` — 포트·백업 로직·Local 어댑터까지 구현 완료(머지됨). 본 작업은 그 문서의 §159 "구현 순서 8. (후속 별건) `S3MemberBackupStorage` + prod 프로필" 을 실제로 착수한다.

---

## 1. 배경 / 문제

탈퇴 정리 배치(`memberWithdrawalCompleteJob`)는 정리 직전 회원 전체 스냅샷을 백업한다. 현재 저장 어댑터는 **Local(로컬 파일시스템)뿐**이라 운영(prod)에서 회원 백업 파일이 애플리케이션 인스턴스의 로컬 디스크에 쌓인다 — 인스턴스 교체/스케일아웃 시 유실되고, 접근통제·보존주기(라이프사이클)도 걸 수 없다. 분쟁 근거자료라는 백업의 목적(202606 §36)을 충족하려면 **prod에서 S3에 저장**해야 한다.

포트/도메인/배치 파이프라인은 이미 완성돼 있고, **저장 어댑터 한 종(S3)과 전환 와이어링, 테스트만** 추가하면 된다.

### 현재 확정된 구조 (조사 결과)

| 요소 | 위치 | 시그니처/조건 |
|---|---|---|
| 포트 | `domain/ma-domain-core/.../withdrawal/domain/port/MemberBackupStorage.kt` | `fun store(directory: String, fileName: String, content: ByteArray)` |
| 호출부 | `.../withdrawal/domain/MemberBackupArchiver.kt` | `storage.store("withdrawal-backup", "${'$'}{member.id}.json", content)` |
| Local 어댑터 | `infrastructure/support/ma-file-storage/.../storage/LocalMemberBackupStorage.kt` | **현재 무조건 `@Component`** (`backup.local.base-path`) |
| 참조 패턴(사진) | 같은 모듈 `S3FileStorage` / `LocalFileStorage` | `@ConditionalOnProperty("file.storage.mode", "s3" / "local"+matchIfMissing)` |
| S3Client 빈 | 같은 모듈 `config/S3StorageConfig.kt` | `@ConditionalOnProperty("file.storage.mode","s3")` — `S3Client` + `S3Presigner` 생성, `file.s3.region` 주입 |
| AWS SDK | `ma-file-storage/build.gradle.kts` | `bom:2.46.20` + `s3` **이미 존재** (추가 불필요) |
| 배치 통합테스트 | `boot/ma-boot-batch/.../MemberWithdrawalCompleteJobIntegrationTest.kt` | `@ActiveProfiles("test")`, `backup.local.base-path` 로 파일 존재 검증 |

> ⚠️ 조사 중 확인된 사실: `file.storage.mode` / `file.s3.*` 키는 **레포 내 어떤 yml에도 없다**. 즉 사진 S3 기능은 prod 외부 주입 전제이고, local/test 는 `matchIfMissing=true` 로 Local 로 동작한다. 백업도 같은 규약을 따른다.

---

## 2. 확정 결정 사항 (6)

### 결정 1 — 전환 메커니즘: `backup.storage.mode` 프로퍼티 (신규 전용 키) · `@ConditionalOnProperty`

- **채택**: 사진 저장과 **같은 메커니즘(`@ConditionalOnProperty`)**을 쓰되, **전용 키 `backup.storage.mode`**(`local` 기본 `matchIfMissing=true` / `s3`)를 신설한다. 202606 메모의 `@Profile("prod")`는 **폐기**.
- **근거**:
  - 이 레포의 실제 관례는 `@Profile`이 아니라 `@ConditionalOnProperty`다(사진 저장 전례). `@Profile("prod")`는 저장소 선택을 배포환경 이름에 묶어버려 — local에서 S3 스모크 테스트 불가, "prod"가 아닌 스테이징에서 S3 사용 불가 — 유연성이 떨어진다.
  - **전용 키를 쓰는 이유(사진 키 `file.storage.mode` 재사용 안 함)**: 백업과 사진은 버킷·보존주기·접근통제가 다른 독립 관심사(결정 3). 한 키로 묶으면 "사진=s3 + 백업=local" 또는 그 반대를 표현할 수 없다. 전용 키라야 두 저장소를 **독립적으로** 전환할 수 있다. → "사진 패턴과 정렬"의 의미 = *같은 방식, 별도 키*.
- **필수 변경 — `LocalMemberBackupStorage`에 조건 추가**: 현재 무조건 `@Component`라, S3 어댑터도 등록되면 `MemberBackupStorage` 타입 빈이 2개가 되어 `MemberBackupArchiver` 주입 지점에서 `NoUniqueBeanDefinitionException`이 난다. 따라서 Local 에 `havingValue="local", matchIfMissing=true`, S3 에 `havingValue="s3"`(matchIfMissing 없음)를 걸어 **정확히 하나만** 활성화한다. (선택이 아니라 필수.)
- **기존 테스트 영향 없음(검증)**: 배치 통합테스트는 `@ActiveProfiles("test")`이고 `backup.storage.mode`를 설정하지 않는다 → `matchIfMissing=true` 로 Local 빈이 그대로 등록 → GREEN 유지. `LocalMemberBackupStorageTest`는 생성자 직접 호출이라 조건과 무관.

### 결정 2 — S3Client 빈 공유: 단일 `S3Client`를 사진·백업이 공유, 클라이언트 프로바이더 활성 조건을 OR로 확장 (옵션 a)

문제: `S3StorageConfig`의 `S3Client`는 `file.storage.mode=s3`에만 생성된다. **백업만 S3**(`backup.storage.mode=s3`, `file.storage.mode` 미설정)면 `S3Client` 빈이 없어 `S3MemberBackupStorage` 와이어링이 실패한다.

| 옵션 | 방식 | 트레이드오프 | 판정 |
|---|---|---|---|
| **(a)** | `S3StorageConfig` 활성 조건을 "둘 중 하나라도 s3"로 확장(`@ConditionalOnExpression`). `S3Client` 단일 빈을 양 어댑터가 공유(버킷은 어댑터별 `@Value` 주입) | 신규 config 0개. 빈 중복·순서 문제 없음. `S3Presigner`가 백업-only-s3일 때 유휴 생성되나 무해(생성 시 네트워크 없음) | ✅ **채택** |
| (b) | 백업 전용 config에서 `@ConditionalOnMissingBean`으로 `S3Client` 보조 생성 | `@ConditionalOnMissingBean`은 `@Configuration` 간 **빈 정의 등록 순서에 의존**해 비결정적(Spring 공식 경고). 순서 뒤집히면 중복/누락 | ❌ 순서 함정 |
| (c) | prod에서 사진·백업을 항상 함께 s3로 켠다고 가정하고 기존 빈 재사용, config 무변경 | 가장 싸지만 백업 가용성이 "사진이 s3임"에 결합. 운영 실수(사진만 local)로 백업 와이어링 붕괴 | ❌ 취약한 전제 |

- **채택 = (a)**: `S3Client`는 region만 필요한 범용 AWS 클라이언트(버킷 미포함)라 진짜로 공유 가능하다. 조건을 다음으로 바꾼다:
  ```kotlin
  @ConditionalOnExpression(
      "'\${file.storage.mode:local}' == 's3' or '\${backup.storage.mode:local}' == 's3'"
  )
  ```
- **`S3Presigner` 처리**: presigner는 사진 URL 서명(`S3FileUrlResolver`, `file.storage.mode=s3` 조건)에만 쓰인다. (a)로 config 전체를 켜면 백업-only-s3일 때 presigner가 유휴로 뜬다 — **무해**(빌더가 network 호출 안 함)하므로 이번 범위에서는 그대로 둔다. 완전 정리를 원하면 `s3Presigner` 빈만 `file.storage.mode=s3` 조건으로 분리하는 것을 **선택적 후속**으로 남긴다(범위 밖).
- **region 프로퍼티**: `S3StorageConfig`는 `file.s3.region`을 주입받는다. (a)로 백업-only-s3를 켜도 이 키가 필요하므로, prod 백업 설정에 `file.s3.region`을 반드시 포함한다(결정 6). 사진 결합처럼 보이는 이 키명은 향후 `aws.s3.region` 같은 공용명으로 개명 가능하나 이번엔 최소 변경 유지.

### 결정 3 — 버킷 분리: 전용 프로퍼티 `backup.s3.bucket` 신설

- **채택**: 백업 전용 `backup.s3.bucket`. 사진의 `file.s3.bucket`과 분리.
- **근거**(202606 §110·§166): 백업은 사진과 **버킷·보존주기·라이프사이클·접근통제가 다르다**. 백업은 (마스킹됐어도) PII를 담아 더 엄격한 접근통제 + Glacier 이관 + 보존만료가 필요하고, 사진은 사용자 노출용이라 수명주기가 다르다. 전용 버킷이라야 버킷 단위 정책(암호화·라이프사이클·정책)을 독립 적용할 수 있다.
- **region**: 이번엔 `file.s3.region`을 **공유**(단일 S3Client). ⚠️ 트랩: S3Client는 region-bound이므로 **백업 버킷은 `file.s3.region`과 같은 리전**에 생성해야 한다(다른 리전이면 요청 리다이렉트/실패). 백업을 별도 리전에 둬야 하면 → 전용 `backup.s3.region` + 전용 client가 필요(후속). 지금은 "동일 리전" 제약을 문서로 명시.

### 결정 4 — 암호화/스토리지 클래스: 코드 미지정, 버킷 정책·라이프사이클(인프라)에 위임

- **채택**: 어댑터 코드에서 SSE(서버측 암호화)·storage class를 **지정하지 않는다**. 기존 `S3FileStorage`와 동일하게 `bucket + key + bytes(+contentType)`만 `putObject`.
- **근거**:
  - **암호화**: 버킷 **기본 암호화(SSE-S3/AES256 또는 SSE-KMS)**를 버킷 정책으로 켜면 모든 put에 자동 적용 → 코드 불필요. per-put 지정은 버킷 정책과 이중관리가 되고 실수 여지만 늘린다.
  - **스토리지 클래스**: Standard→Glacier 전환은 **S3 Lifecycle 규칙**(경과일 기준)으로 처리. per-put 으로 즉시 Glacier에 넣으면 재시도 시 즉시 읽기가 불가(Glacier 복원 지연)해지고 운영이 경직된다.
  - 결과적으로 어댑터는 사진 어댑터와 **동일한 형태**를 유지(일관성).
- **인프라 체크리스트(코드 외, 후속·운영)**: 백업 버킷에 ① 기본 암호화 ON, ② 라이프사이클(예: N일 후 Glacier, M일 후 만료), ③ 최소권한 버킷 정책/차단 퍼블릭 액세스. 이 설정 없이 배포하면 "암호화 위임"이 무의미하므로 배포 전 확인 필수.

### 결정 5 — 테스트 전략: `mockk<S3Client>` 단위 테스트 (TDD, `S3FileStorageTest` 패턴), 실 S3 통합/스모크는 범위 밖

- **채택**: `S3MemberBackupStorageTest`를 **먼저 작성(RED)** 후 구현(GREEN). `S3FileStorageTest`와 동형:
  - `PutObjectRequest` slot 캡처 → `bucket() == backup.s3.bucket`, `key() == "$directory/$fileName"`, `contentType() == "application/json"` 검증.
  - `RequestBody`로 전달된 바이트가 `content`와 동일한지 검증(캡처 후 비교).
- **범위 밖**: 실 AWS 대상 통합/스모크 테스트(테스트 자격증명·비용·플레이키). Local 경로 종단검증은 기존 배치 통합테스트가 이미 커버, S3 경로는 단위로 충분.
- **최초 prod 배포 시 수동 스모크(문서)**: 실제 버킷에 put → 콘솔/CLI로 read-back 1건 확인(자격증명·버킷·리전·암호화 실동작 점검). 자동화 아님.

### 결정 6 — 설정 파일 변경 범위: local/test 무변경, prod 필요 키 문서화

- **local/test yml**: **변경 없음**. `matchIfMissing=true`로 Local이 기본 → `backup.storage.mode` 불필요. s3 키(자격증명·버킷)는 로컬에 두지 않는다.
- **prod (레포에 없음, 외부 주입) — 필요 키 목록**:
  | 키 | 값 | 비고 |
  |---|---|---|
  | `backup.storage.mode` | `s3` | 백업만 켜도 결정 2로 S3Client 생성됨 |
  | `backup.s3.bucket` | `<백업 버킷명>` | 결정 3, 사진 버킷과 분리 |
  | `file.s3.region` | `<리전>` | 공유 region. 백업-only-s3여도 필수(결정 2·3). 백업 버킷은 이 리전에 생성 |
  | (사진도 s3 시) `file.storage.mode=s3`, `file.s3.bucket` | | 백업과 독립 |
  - 자격증명은 yml에 넣지 않고 AWS 기본 자격증명 체인(IAM Role/인스턴스 프로파일/env)로 주입 — 기존 `S3StorageConfig`가 `.credentialsProvider`를 지정하지 않으므로 default chain을 그대로 따른다.

---

## 3. 목표 구조 (아키텍처)

```
[batch] MemberBackupItemProcessor.process(member)
            └─ MemberBackupArchiver.archive(member)            (domain @Component, 무변경)
                 ├─ collector.collect(member) → MemberWithdrawalBackup
                 ├─ serializer.serialize(backup) → ByteArray(JSON)
                 └─ storage.store("withdrawal-backup", "{id}.json", content)
                          │  (port: MemberBackupStorage, 무변경)
                          ▼
        ┌─────────────────────────────────────────────────────────┐
        │  구현체 선택 = @ConditionalOnProperty("backup.storage.mode") │
        │                                                           │
        │  local(기본)                     s3                         │
        │  ─────────────                   ─────────────             │
        │  LocalMemberBackupStorage        S3MemberBackupStorage ★신규 │
        │   (조건 추가)                      (S3Client 주입,            │
        │   basePath 파일쓰기                putObject bucket/key)      │
        └────────────────────────────────────┬──────────────────────┘
                                              │ needs S3Client
                                              ▼
                    S3StorageConfig  (조건 확장: file.mode=s3 OR backup.mode=s3)
                       └─ @Bean S3Client(region = file.s3.region)   ← 사진·백업 공유
                       └─ @Bean S3Presigner (사진 전용, 백업-only-s3 시 유휴·무해)
```

색: batch/domain = 무변경, `★` = 신규, "조건 추가/확장" = 수정.

---

## 4. 영향 파일

### 신규
| # | 파일 | 내용 |
|---|---|---|
| N1 | `infrastructure/support/ma-file-storage/src/test/kotlin/com/konkuk/ma/storage/S3MemberBackupStorageTest.kt` | `mockk<S3Client>` + `PutObjectRequest` slot 검증 (**선작성, RED**) |
| N2 | `infrastructure/support/ma-file-storage/src/main/kotlin/com/konkuk/ma/storage/S3MemberBackupStorage.kt` | S3 어댑터 (**GREEN**) |

### 수정
| # | 파일 | 변경 | 이유 |
|---|---|---|---|
| M1 | `.../ma-file-storage/.../storage/LocalMemberBackupStorage.kt` | 클래스에 `@ConditionalOnProperty(name=["backup.storage.mode"], havingValue="local", matchIfMissing=true)` 추가 | 빈 중복(`NoUniqueBeanDefinitionException`) 방지 — 결정 1 |
| M2 | `.../ma-file-storage/.../config/S3StorageConfig.kt` | 클래스 조건을 `@ConditionalOnProperty(file.storage.mode=s3)` → `@ConditionalOnExpression("… file.mode==s3 or backup.mode==s3")` | 백업-only-s3에서도 S3Client 공급 — 결정 2 |

### 무변경 (참고)
- 포트 `MemberBackupStorage`, `MemberBackupArchiver`, `MemberBackupItemProcessor`, 배치 JobConfig, `build.gradle.kts`(AWS SDK 기존), local/test yml.

---

## 5. 파일별 상세 설계

### N2. `S3MemberBackupStorage.kt` (신규)

```kotlin
package com.konkuk.ma.storage

import com.konkuk.ma.domain.withdrawal.domain.port.MemberBackupStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
@ConditionalOnProperty(name = ["backup.storage.mode"], havingValue = "s3")
class S3MemberBackupStorage(
    private val s3Client: S3Client,
    @Value("\${backup.s3.bucket}")
    private val bucket: String,
) : MemberBackupStorage {

    override fun store(directory: String, fileName: String, content: ByteArray) {
        val key = "$directory/$fileName"
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(CONTENT_TYPE_JSON)
            .build()
        s3Client.putObject(request, RequestBody.fromBytes(content))
    }

    companion object {
        private const val CONTENT_TYPE_JSON = "application/json"
    }
}
```

- **키 조립**: 호출부(`MemberBackupArchiver`)가 `directory="withdrawal-backup"`, `fileName="{memberId}.json"`을 넘기므로 S3 키 = `withdrawal-backup/{memberId}.json`. 날짜 파티션 등 키 레이아웃은 **호출부 소관**이라 어댑터는 `directory/fileName` 연결만 한다(202606의 date 파티션은 미도입 상태 — 범위 밖).
- **멱등성**: S3 `putObject`는 동일 키 덮어쓰기 → Local 어댑터의 덮어쓰기 동작과 일치 → 재시도 안전(202606 §103).
- **`contentType("application/json")`**: 백업은 항상 JSON이라 명시(다운로드/뷰어 편의). ⚠️ 기존 `S3FileStorage.storeBytes`는 contentType을 `null`로 넘기므로 이는 의도적 차이 — "포맷을 아는 곳에서 정확히 표기"라는 정당한 선택. 엄격 미러링을 원하면 이 한 줄만 제거 가능(무해).
- **생성자 파라미터 설계**: `s3Client`는 공유 빈 주입(결정 2), `bucket`은 어댑터별 전용 값(`backup.s3.bucket`, 결정 3)이라 `@Value`로 어댑터에 국소화 — `S3FileStorage`와 동일한 형태.

### M1. `LocalMemberBackupStorage.kt` (수정: 조건 1줄 추가)

```kotlin
@Component
@ConditionalOnProperty(name = ["backup.storage.mode"], havingValue = "local", matchIfMissing = true)  // ← 추가
class LocalMemberBackupStorage(
    @Value("\${backup.local.base-path:backups}")
    private val basePath: String,
) : MemberBackupStorage {
    // 본문 무변경
}
```
- import 추가: `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`.

### M2. `S3StorageConfig.kt` (수정: 조건 확장)

```kotlin
@Configuration
@ConditionalOnExpression(
    "'\${file.storage.mode:local}' == 's3' or '\${backup.storage.mode:local}' == 's3'"  // ← 변경
)
class S3StorageConfig(
    @Value("\${file.s3.region}")
    private val region: String,
) {
    @Bean fun s3Client(): S3Client = S3Client.builder().region(Region.of(region)).build()
    @Bean fun s3Presigner(): S3Presigner = S3Presigner.builder().region(Region.of(region)).build()
}
```
- import 교체: `ConditionalOnProperty` → `org.springframework.boot.autoconfigure.condition.ConditionalOnExpression`.
- 나머지 무변경.

### N1. `S3MemberBackupStorageTest.kt` (신규, 선작성 RED)

`S3FileStorageTest` 패턴(FunSpec + `mockk<S3Client>` + `slot<PutObjectRequest>`)을 따른다. 검증 케이스:
1. `store(directory, fileName, content)` → `bucket == "test-backup-bucket"`, `key == "$directory/$fileName"`, `contentType == "application/json"` 로 `putObject` 1회 호출.
2. 전달 바이트가 `content`와 동일(`RequestBody` 캡처 후 비교, 또는 `putObject` 두 번째 인자 `slot` 캡처).
3. (선택) 같은 키 재호출 시에도 예외 없이 `putObject` 재호출(멱등성은 S3 계약이므로 mock에선 "동일 키로 재호출됨"만 확인).

> KoTest/Mockk 세부는 `kotest-writing` 스킬 참조. `RequestBody` 바이트 비교가 번거로우면 케이스 1(요청 필드 검증)만으로도 어댑터 계약은 충분히 고정된다.

---

## 6. 구현 순서 (TDD: RED → GREEN)

| # | 단계 | 파일 | 검증 |
|---|---|---|---|
| 1 | 브랜치 확인/체크아웃 `feat/withdrawal-backup-s3-adapter` | — | — |
| 2 | **테스트 선작성** `S3MemberBackupStorageTest` | N1 | 컴파일은 되나 어댑터 부재로 **RED** |
| 3 | 어댑터 구현 `S3MemberBackupStorage` | N2 | 2의 테스트 **GREEN** |
| 4 | Local 어댑터에 조건 추가 | M1 | 모듈 컴파일 |
| 5 | `S3StorageConfig` 조건 확장 | M2 | 모듈 컴파일 |
| 6 | 모듈 테스트 `:infrastructure:support:ma-file-storage:test` | — | 전 GREEN (기존 `LocalMemberBackupStorageTest`·`S3FileStorageTest`·`Jackson…Test` 포함) |
| 7 | 배치 회귀 `:boot:ma-boot-batch:test` | — | `MemberWithdrawalCompleteJobIntegrationTest` GREEN (test 프로필=Local 유지 검증) |
| 8 | 전체 빌드 `./gradlew build` | — | GREEN |
| 9 | code-reviewer 검증 후 반영 | — | clean-architecture / code-implementation-rules / clean-code |
| 10 | prod 설정 키 목록(결정 6) + 인프라 체크리스트(결정 4)를 배포 담당에 전달 | 문서 | — |

> 실 S3 대상 `local-verify`는 이번 범위 아님(자격증명 부재). 단위·회귀 GREEN + 리뷰 반영까지가 완료 기준.

---

## 7. 리스크 / 주의

- **빈 중복(치명)**: M1을 빠뜨리면 s3 모드에서 `MemberBackupStorage` 빈 2개 → 부팅 실패. → 순서 4를 반드시 포함, 순서 6/7에서 부팅으로 검증.
- **리전 불일치(트랩)**: 공유 S3Client는 `file.s3.region` 리전. 백업 버킷을 다른 리전에 만들면 put 실패/리다이렉트 → 결정 3의 "동일 리전" 제약을 배포 체크리스트에 명시. 다른 리전 필요 시 전용 client 후속.
- **`S3StorageConfig` region 키 의존**: 백업-only-s3여도 `file.s3.region` 필수(결정 2). 이 키 누락 시 config 생성 실패 → 결정 6 표에 포함.
- **암호화 위임의 전제**: 결정 4는 버킷 기본 암호화·라이프사이클이 인프라에 설정돼 있어야 성립. 미설정 배포 시 평문·무제한 보존 → 배포 전 인프라 체크리스트 확인 필수.
- **유휴 S3Presigner**: 백업-only-s3에서 presigner 빈이 뜨지만 무해. 정리 원하면 presigner 분리(선택적 후속).

---

## 8. 범위 밖 (후속으로만 기록)

- 포트 `MemberBackupStorage` 시그니처 변경, 백업 키에 날짜 파티션 도입(`withdrawal-backup/{date}/{id}.json`), collector/serializer/도메인 로직 변경.
- 보존주기·라이프사이클·버킷 정책·기본 암호화의 **실제 인프라 프로비저닝**(IaC/콘솔) — 코드 아님.
- `backup.s3.region` 전용 키 + 전용 client(백업을 다른 리전에 둘 때).
- `s3Presigner` 빈을 `file.storage.mode=s3` 전용 조건으로 분리(백업-only-s3의 유휴 빈 제거).
- 실 S3 통합/스모크 자동화, prod 최초 배포 read-back 스모크 절차.

---

## 9. 구현 시 참조

- 포트/어댑터 배치·명명: `clean-architecture` (§4-1 어댑터, §9 치트시트) — 어댑터는 `ma-file-storage`, 구현 기술 접두 `S3` + 역할명.
- 포트/구현 규칙·네이밍·성능: `code-implementation-rules` (§6 포트, §9 하드코딩 상수화, §13 설정 배치).
- 테스트(KoTest/Mockk 슬롯 캡처): `kotest-writing`, 참조 `S3FileStorageTest.kt`.
- 선행 설계·결정 이력: `docs/plan/202606/withdrawal-data-backup.plan.md`.
- 후속 정합화(사진 S3 전환 등): `docs/plan/complete/xroom-media-cleanup.plan.md` "S3 전환" 절.
