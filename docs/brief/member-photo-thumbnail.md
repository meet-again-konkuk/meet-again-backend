# Member Photo 업로드 시 썸네일 생성 - 간략 구현 계획

## 설계 개요

프로필 사진 업로드 시 `ThumbnailGenerator` 포트를 통해 가로 400px 썸네일을 생성하고, 원본과 별도 경로에 저장하여 DB에 `thumbnailPath`를 함께 관리한다. 썸네일 생성에는 **Thumbnailator + TwelveMonkeys** 조합을 사용하여 WebP, SVG 등 다양한 포맷을 지원한다.

## 기술 스택

- **Thumbnailator** (`net.coobird:thumbnailator:0.4.20`) — 리사이징/썸네일 생성 (간결한 API, 고품질 리사이징)
- **TwelveMonkeys ImageIO WebP** (`com.twelvemonkeys.imageio:imageio-webp:3.11.0`) — WebP 포맷 지원 (ImageIO SPI 자동 등록)
- **TwelveMonkeys ImageIO Batik** (`com.twelvemonkeys.imageio:imageio-batik:3.11.0`) — SVG 포맷 지원

TwelveMonkeys는 ImageIO에 SPI로 자동 등록되어, 의존성 추가만으로 Thumbnailator가 내부적으로 사용하는 `ImageIO.read()`/`ImageIO.write()`에서 WebP/SVG를 처리한다. 별도 코드 변경 불필요.

## 변경/신규 파일 목록

| 파일 경로 | 변경 유형 | 설명 |
|-----------|-----------|------|
| `domain/ma-domain-core/.../file/port/ThumbnailGenerator.kt` | 신규 | 썸네일 생성 포트 인터페이스 (`generate(source: ByteArray, width: Int): ByteArray`) |
| `domain/ma-domain-core/.../file/port/FileStorage.kt` | 수정 | `storeBytes()` 메서드 추가 (썸네일은 PhotoFile이 아닌 ByteArray이므로 별도 저장 메서드 필요) |
| `domain/ma-domain-core/.../file/StorageUsageType.kt` | 수정 | `THUMBNAIL("thumbnail")` enum 값 추가 |
| `domain/ma-domain-core/.../photo/NewPhoto.kt` | 수정 | `thumbnailPath: String?` 필드 추가 |
| `domain/ma-domain-core/.../photo/MemberPhoto.kt` | 수정 | `thumbnailPath: String?` 필드 + `hasThumbnail()` 메서드 추가 |
| `domain/ma-domain-core/.../application/MemberPhotoService.kt` | 수정 | `ThumbnailGenerator` 의존성 추가, 업로드 시 썸네일 생성/저장, 삭제 시 썸네일 함께 삭제 |
| `infrastructure/support/ma-file-storage/build.gradle.kts` | 수정 | Thumbnailator + TwelveMonkeys 의존성 추가 |
| `infrastructure/support/ma-file-storage/.../ThumbnailatorThumbnailGenerator.kt` | 신규 | Thumbnailator 기반 `ThumbnailGenerator` 구현체 |
| `infrastructure/support/ma-file-storage/.../LocalFileStorage.kt` | 수정 | `storeBytes()` 메서드 구현 |
| `infrastructure/storage/ma-db-core/.../table/MemberPhotoTable.kt` | 수정 | `thumbnailPath` 컬럼 추가 (nullable) |
| `infrastructure/storage/ma-db-core/.../entity/MemberPhotoEntity.kt` | 수정 | `thumbnailPath` 필드 추가, `toDomain()` 수정 |
| `infrastructure/storage/ma-db-core/.../dao/MemberPhotoCommandDao.kt` | 수정 | `save()`에 `thumbnailPath` 삽입 추가 |
| `infrastructure/storage/ma-db-core/.../resources/script/ddl.sql` | 수정 | `THUMBNAIL_PATH VARCHAR(512) NULL` 컬럼 추가 |

## 핵심 설계 결정

1. **Thumbnailator + TwelveMonkeys 조합**: JDK 내장 AWT 대신 Thumbnailator를 사용하여 고품질 리사이징 + 간결한 API. TwelveMonkeys는 SPI 자동 등록으로 WebP/SVG 포맷을 투명하게 지원. 현재 `AllowedExtension`의 jpg, jpeg, png, svg, webp를 모두 커버한다.
2. **썸네일 생성 실패 시 원본 저장은 진행**: `generateThumbnail()`을 try-catch로 감싸 실패 시 `thumbnailPath = null`로 처리. 원본 업로드가 썸네일 생성 실패에 영향받지 않도록 한다.
3. **도메인 객체에 행위 부여**: `MemberPhoto.hasThumbnail()`로 썸네일 존재 여부를 객체 스스로 판단. 삭제 로직에서 외부가 `thumbnailPath != null`을 직접 검사하지 않고 도메인 메서드에 위임한다.

## 구현 순서

1. **의존성 추가**: `ma-file-storage/build.gradle.kts`에 Thumbnailator + TwelveMonkeys 의존성 추가
2. **도메인 포트/모델 변경**: `ThumbnailGenerator` 포트 신규 생성 -> `FileStorage`에 `storeBytes()` 추가 -> `StorageUsageType.THUMBNAIL` 추가 -> `NewPhoto`, `MemberPhoto`에 `thumbnailPath` 필드 추가
3. **인프라 DB 변경**: `MemberPhotoTable`, `MemberPhotoEntity`, `MemberPhotoCommandDao`에 `thumbnailPath` 반영 -> DDL 수정
4. **인프라 구현체**: `ThumbnailatorThumbnailGenerator` 신규 생성 -> `LocalFileStorage.storeBytes()` 구현
5. **서비스 로직 수정**: `MemberPhotoService.upload()`에 썸네일 생성/저장 로직 추가, `delete()`에 썸네일 삭제 로직 추가
6. **테스트**: `ThumbnailatorThumbnailGenerator` 단위 테스트 -> `MemberPhotoService` 단위 테스트 (썸네일 생성 성공/실패 케이스)
