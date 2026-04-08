# Brief: 게시글 좋아요

> 작성일: 2026-04-08 (수정)

## 목표
게시글에 좋아요 추가/취소 API를 구현한다. 댓글 좋아요(CommentLike)와 동일한 패턴을 적용한다.

## API
- **POST** `/api/community/posts/{postId}/likes` — 좋아요 추가 (인증 필요, 200 OK)
- **DELETE** `/api/community/posts/{postId}/likes` — 좋아요 취소 (인증 필요, 200 OK)

## 설계 결정
- **like/unlike 분리**: 토글 방식이 아닌 POST(생성)/DELETE(삭제)로 RESTful 설계
- **likes 비정규화 유지**: Post.likes 카운트 컬럼을 유지하고, UPDATE SET likes = likes + 1로 동시성 처리 (댓글 좋아요와 동일)
- **CommentLike 패턴 그대로 적용**: PostLike 도메인, PostLikeTable, PostLikeDao, PostLikeRepository, PostLikeCoreRepository

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `ddl.sql` | 수정 | COMMUNITY_POST_LIKES 테이블 추가 |
| `domain/community/domain/PostLike.kt` | 신규 | PostLike 도메인 모델 (postId, memberEmail) |
| `domain/community/domain/PostLikeResult.kt` | 신규 | 좋아요 결과 (liked, likeCount) |
| `domain/community/domain/port/PostLikeRepository.kt` | 신규 | 포트 인터페이스 - save, delete |
| `domain/community/domain/port/PostCommandRepository.kt` | 수정 | increaseLikes(postId): Int, decreaseLikes(postId): Int 추가 |
| `domain/community/application/PostLikeService.kt` | 신규 | like(), unlike() 메서드 |
| `entity/table/PostLikeTable.kt` | 신규 | COMMUNITY_POST_LIKES 테이블 정의 |
| `entity/PostLikeEntity.kt` | 신규 | Entity + toDomain(), from(ResultRow) |
| `dao/PostLikeDao.kt` | 신규 | save, delete DAO |
| `dao/PostCommandDao.kt` | 수정 | increaseLikes, decreaseLikes (UPDATE 쿼리 + 변경 후 카운트 반환) |
| `repository/PostLikeCoreRepository.kt` | 신규 | PostLikeRepository 구현체 |
| `repository/PostCommandCoreRepository.kt` | 수정 | increaseLikes, decreaseLikes 구현 |
| `api/PostLikeApi.kt` | 신규 | POST/DELETE /api/community/posts/{postId}/likes |
| `api/response/PostLikeResponse.kt` | 신규 | liked(Boolean), likeCount(Int) 응답 |

## 구현 순서
1. DDL에 COMMUNITY_POST_LIKES 테이블 추가
2. PostLike 도메인 모델 + PostLikeResult 생성
3. PostLikeRepository 포트 인터페이스 (save, delete)
4. PostCommandRepository에 increaseLikes/decreaseLikes 추가
5. 인프라 계층 — PostLikeTable, PostLikeEntity, PostLikeDao, PostLikeCoreRepository
6. PostCommandDao/PostCommandCoreRepository에 likes 증감 구현
7. PostLikeService 구현 (like, unlike)
8. PostLikeApi + PostLikeResponse 구현

## 주의사항
- CommentLike 패턴을 그대로 따른다 (like/unlike 분리, 비정규화 유지)
- COMMUNITY_POST_LIKES 테이블에 (POST_ID, MEMBER_EMAIL) 복합 유니크 인덱스
- DDL에 FK 사용 금지 (PK + INDEX만 사용)
- likes 카운트 증감은 UPDATE 쿼리로 직접 처리 (동시성 고려)
- increaseLikes/decreaseLikes는 변경 후 likeCount를 반환
