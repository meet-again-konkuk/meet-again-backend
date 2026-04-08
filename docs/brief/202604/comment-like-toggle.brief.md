# Brief: 댓글 좋아요 토글

> 작성일: 2026-04-08

## 목표
인증된 사용자가 댓글에 좋아요를 누르거나 취소(토글)할 수 있도록 한다. 좋아요 이력을 별도 테이블로 관리하여 중복 좋아요를 방지하고, Comment에 likes 카운트를 비정규화하여 조회 성능을 확보한다.

## API
- **POST** `/api/community/comments/{commentId}/like` (인증 필요, 200 OK)
- 기존 댓글 URL(`/api/community/posts/{postId}/comments`)은 2단계 중첩이므로, 좋아요는 comment 리소스 기준으로 1단계로 설계

## 변경 파일
| 파일 | 변경 | 설명 |
|------|------|------|
| `ddl.sql` | 수정 | COMMUNITY_COMMENT_LIKES 테이블 추가, COMMUNITY_COMMENTS에 LIKES 컬럼 추가 |
| `domain/community/domain/CommentLike.kt` | 신규 | 댓글 좋아요 도메인 모델 (commentId, memberEmail) |
| `domain/community/domain/port/CommentLikeRepository.kt` | 신규 | 포트 인터페이스 - findOne(commentId, email), save, delete |
| `domain/community/domain/port/CommentCommandRepository.kt` | 수정 | increaseLikes(commentId), decreaseLikes(commentId) 메서드 추가 |
| `domain/community/domain/Comment.kt` | 수정 | likes 필드 추가 |
| `domain/community/application/CommentLikeService.kt` | 신규 | toggle(commentId, email) - 좋아요 존재 여부 확인 후 추가/삭제 조합 |
| `entity/table/CommentLikeTable.kt` | 신규 | COMMUNITY_COMMENT_LIKES 테이블 정의 |
| `entity/table/CommentTable.kt` | 수정 | likes 컬럼 추가 |
| `entity/CommentLikeEntity.kt` | 신규 | Entity + toDomain(), from(ResultRow) |
| `entity/CommentEntity.kt` | 수정 | likes 매핑 추가 |
| `dao/CommentLikeDao.kt` | 신규 | findOne, save, delete DAO |
| `dao/CommentCommandDao.kt` | 수정 | increaseLikes, decreaseLikes 메서드 추가 |
| `repository/CommentLikeCoreRepository.kt` | 신규 | CommentLikeRepository 구현체 |
| `repository/CommentCommandCoreRepository.kt` | 수정 | increaseLikes, decreaseLikes 구현 |
| `api/CommunityCommentLikeApi.kt` | 신규 | POST /api/community/comments/{commentId}/like 엔드포인트 |
| `api/response/CommentLikeResponse.kt` | 신규 | liked(Boolean), likeCount(Int) 응답 |
| **테스트** | | |
| `application/CommentLikeServiceTest.kt` | 신규 | toggle 로직 단위 테스트 |
| `api/CommunityCommentLikeApiTest.kt` | 신규 | API 통합 테스트 |
| `fixture/CommentLikeFixture.kt` | 신규 | 테스트 픽스처 |

## 구현 순서
1. DDL 수정 - COMMUNITY_COMMENT_LIKES 테이블 생성, COMMUNITY_COMMENTS에 LIKES 컬럼 추가
2. CommentLike 도메인 모델 + CommentLikeRepository 포트 정의
3. CommentCommandRepository에 increaseLikes/decreaseLikes 추가, Comment에 likes 필드 추가
4. 인프라 계층 구현 - CommentLikeTable, CommentLikeEntity, CommentLikeDao, CommentLikeCoreRepository
5. CommentCommandDao/CommentCommandCoreRepository에 likes 증감 구현, CommentTable/CommentEntity 수정
6. CommentLikeService 구현 (toggle 조합 로직)
7. CommunityCommentLikeApi + CommentLikeResponse 구현
8. 테스트 작성 (Service 단위 테스트, API 통합 테스트)

## 주의사항
- CommentLikeRepository.findOne은 nullable 반환 (`findOneOrNull`) - 좋아요가 없는 상태가 정상이므로
- COMMUNITY_COMMENT_LIKES 테이블에 (COMMENT_ID, MEMBER_EMAIL) 복합 유니크 인덱스 필요
- DDL에 FK 사용 금지 (PK + INDEX만 사용)
- likes 카운트 증감은 UPDATE 쿼리로 직접 처리 (동시성 고려, 객체 로딩 없이)
- 게시글 좋아요 토글(POST /api/community/posts/{postId}/like)도 동일 패턴이므로, 댓글 좋아요 구현 후 재활용 가능한 구조 고려
