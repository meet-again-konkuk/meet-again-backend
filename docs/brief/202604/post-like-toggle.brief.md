# Brief: 게시글 좋아요 토글

> 작성일: 2026-04-06

## 목표
게시글에 좋아요를 토글(추가/취소)하는 API를 구현한다. CommentLike와 동일한 패턴으로 PostLike를 추가하며, PostTable.likes 비정규화 컬럼을 제거하고 POST_LIKES 테이블의 count로 대체한다.

## 설계 결정
- **likes 비정규화 제거**: comments와 동일하게 likes 컬럼을 제거하고 count 쿼리로 처리 (일관성 우선)
- **CommentLike 패턴 그대로 적용**: PostLike 도메인, PostLikeTable, PostLikeDao, PostLikeRepository, PostLikeCoreRepository

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `ddl.sql` | 수정 | COMMUNITY_POST_LIKES 테이블 추가, COMMUNITY_POSTS.LIKES 컬럼 제거 |
| `domain/community/domain/PostLike.kt` | 신규 | PostLike 도메인 모델 (postId, memberEmail) |
| `domain/community/domain/port/PostLikeRepository.kt` | 신규 | 포트 인터페이스 (findOneOrNull, save, delete, count) |
| `domain/community/application/PostCommandService.kt` | 수정 | toggleLike 메서드 추가 (PostLikeRepository 의존 추가) |
| `infrastructure/community/entity/table/PostLikeTable.kt` | 신규 | Exposed 테이블 (POST_ID, MEMBER_EMAIL) |
| `infrastructure/community/entity/PostLikeEntity.kt` | 신규 | Entity + toDomain() + from(ResultRow) |
| `infrastructure/community/dao/PostLikeDao.kt` | 신규 | findOne, save, delete, count DAO |
| `infrastructure/community/repository/PostLikeCoreRepository.kt` | 신규 | 포트 구현체 |
| `infrastructure/community/entity/table/PostTable.kt` | 수정 | likes 컬럼 제거 |
| `infrastructure/community/entity/PostEntity.kt` | 수정 | likes 필드 제거 |
| `infrastructure/community/dao/PostQueryDao.kt` | 수정 | likes 컬럼 참조 제거 |
| `domain/community/domain/Post.kt` | 수정 | likes 필드 제거 |
| `domain/community/domain/PostWithAuthor.kt` | 수정 | likes 필드 추가 (count 결과를 담기 위해) |
| `domain/community/application/PostQueryService.kt` | 수정 | 게시글 조회 시 PostLikeRepository.count로 좋아요 수 조합 |
| `boot/community/api/CommunityPostCommandApi.kt` | 수정 | toggleLike 엔드포인트 추가 |
| `boot/community/api/response/PostResponse.kt` | 수정 | likes를 PostWithAuthor에서 가져오도록 변경 |
| `boot/community/api/CommunityPostCommandApiTest.kt` | 수정 | toggleLike API 테스트 추가 |
| `domain/community/application/PostCommandServiceTest.kt` | 수정 | toggleLike 서비스 테스트 추가 |

## 구현 순서
1. DDL에 COMMUNITY_POST_LIKES 테이블 추가, COMMUNITY_POSTS에서 LIKES 컬럼 제거
2. PostLike 도메인 모델 생성 (CommentLike 패턴 참조)
3. PostLikeRepository 포트 인터페이스 생성 (findOneOrNull, save, delete, count)
4. PostLikeTable, PostLikeEntity, PostLikeDao 인프라 계층 구현
5. PostLikeCoreRepository 포트 구현체 작성
6. PostTable, PostEntity, Post에서 likes 필드 제거
7. PostWithAuthor에 likes 필드 추가, PostQueryService에서 count 조합 로직 추가
8. PostCommandService에 toggleLike 메서드 추가
9. CommunityPostCommandApi에 toggleLike 엔드포인트 추가
10. PostResponse 수정 (PostWithAuthor.likes 참조)
11. 테스트 작성 (서비스 단위 테스트, API 테스트)

## 주의사항
- CommentLikeDao 패턴 그대로 따른다 (findOne, save, delete + count 추가)
- PostLikeTable의 UNIQUE INDEX는 (POST_ID, MEMBER_EMAIL) 복합으로 설정
- Post.likes 제거 시 기존 PostEntity.from(), toDomain(), PostResponse.from() 등 연쇄 수정 필요
- PostQueryService.find()에서 N+1 방지: 게시글 목록의 postId 목록으로 한 번에 count 조회 후 메모리에서 조합
- PostLikeRepository.count는 List<Long> (postIds)를 받아 Map<Long, Int>를 반환하는 벌크 조회 메서드로 설계
