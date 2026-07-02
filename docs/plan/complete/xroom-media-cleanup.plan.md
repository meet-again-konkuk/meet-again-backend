# Plan: X룸 media 파일 생명주기 정리 (2-cleanup)

> 작성일: 2026-07-02 · 상태: Handoff (다음 세션 착수)
> 선행: X룸 "기억의 방" Phase 0~3 전부 머지 완료(develop tip=2beb198). 이건 X룸 기능이 아니라 **후속 정합성/운영** 작업.

## 배경 (왜 필요한가)

Phase 3에서 media는 **전부 soft delete만** 하고 물리 파일은 안 지운다(교체/삭제/기억삭제 연쇄 모두 soft delete, cleanup은 후속). 그 결과 두 구멍이 있다:

1. **평상시**: 사진 교체·삭제·기억 삭제로 soft-deleted된 media의 물리 파일이 디스크에 영구 잔존.
2. **회원 탈퇴 시**: `MemberDataCleaner.cleanXroom` = `xroomCommandRepository.delete(member.id)`(방만 soft delete)뿐 → 그 방의 memory·media는 **연쇄 안 됨**(active 잔존), 백업에도 없음. (참고: member 프로필 사진은 `cleanPhoto`→`MemberPhotoCleaner`가 물리삭제하지만 X룸 media는 그런 처리가 전무.)

## 공통 결정 (먼저 확정)

- **물리삭제 주체는 Part B 배치 하나로 통일** 권장: 탈퇴든 평상시든 media는 **soft delete까지만** 하고, 물리 파일 삭제는 Part B의 cleanup 배치가 유일하게 담당. (탈퇴 시 즉시 물리삭제 = member 사진 방식도 가능하나, 경합·중복 회피 위해 soft-delete 통일이 깔끔.)
- FileStorage는 헥사고날 포트라 물리삭제(`FileStorage.delete`)는 이미 존재(`LocalFileStorage.delete`). S3 전환(plan §9 Phase 4 후반)돼도 Part B는 포트로 동작.

## Part A — 회원 탈퇴 연쇄정리 (withdrawal 도메인)

**목표**: 탈퇴 회원의 방 soft delete 시 그 방의 memory·media도 연쇄 soft delete하고, 백업에 포함.

- `withdrawal/domain/MemberDataCleaner.cleanXroom` 확장: ownerId의 방들 → 각 방의 memory soft delete → 그 memory들의 media soft delete. (물리삭제는 안 함 = 공통 결정)
- 필요 포트(신규): 회원 기준 일괄 정리용 — 예) `MemoryCommandRepository.softDeleteByXrooms(xroomIds, memberId)` / `MediaCommandRepository.softDeleteByMemories(memoryIds, memberId)` 또는 ownerId 기반 조회 후 연쇄. 기존 `MediaCommandRepository.softDeleteByMemory`(단건)·`MemoryQueryRepository.find(xroomId)` 재활용 가능성 검토.
- `withdrawal/domain/MemberWithdrawalBackupCollector`: 백업 스냅샷에 memory·media 포함(현재 `xrooms`만). plan §11 "추천: Phase 2/3에서 백업 항목 추가" 미이행분.
- 테스트: withdrawal 통합/배치 테스트에 "탈퇴→방·기억·media 전부 soft delete + 백업 포함" 케이스.

## Part B — soft-deleted media 파일 cleanup 배치 (ma-boot-batch)

**목표**: soft-deleted media의 물리 파일(storageKey·thumbnailKey)을 스토리지에서 삭제.

- `ma-boot-batch`에 신규 Job(선례: `job/domain/matching/ExpiredMatchingResultDeleteJobConfig`, `job/domain/member/MemberWithdrawalCompleteJobConfig` — reader/processor/writer 청크 패턴).
  - reader: soft-deleted 이면서 아직 파일 미정리인 media 조회.
  - writer(또는 processor): `FileStorage.delete(storageKey)` + thumbnailKey 있으면 삭제 → 정리 완료 표시(플래그) 또는 행 hard delete.
- 필요 포트(신규): `MediaQueryRepository`에 soft-deleted(미정리) media 조회, `MediaCommandRepository`에 hard delete 또는 "파일 정리됨" 표시.
- 판단: 정리 완료를 어떻게 마킹할지(별도 컬럼 vs deletedDate 기준 N일 경과 후 hard delete). DB 메타 구조 변경 최소화.

## 다음 단계(운영, 더 후속) — S3 전환

Part A·B 후: `FileStorage` LocalFileStorage→S3 어댑터 교체 + presigned URL(접근제어). 이때 `Media.toPhotoUrl(baseUrl)`을 URL resolver 포트+어댑터로 승격(서명 생성=S3 클라이언트라는 실제 인프라 의존 생기는 시점). plan §9 Phase 4 / §10-10.

## 참고

- 메모리: `project_xroom_memory_room_redesign.md`(Phase 0~3 이력·결정), plan `docs/plan/xroom-memory-room-redesign.plan.md` §9(Phase 4)·§11(외부도메인 정합화).
- 현재 상태: develop tip=`2beb198`, X룸 Phase 0~3 전부 머지, working tree 클린. 새 브랜치 분기해서 작업(예 `feat/xroom-media-cleanup` 또는 Part별 분리).
