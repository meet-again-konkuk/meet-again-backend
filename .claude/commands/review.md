code-rules-reviewer와 clean-code-reviewer 두 에이전트를 사용하여 다음 대상 코드를 검증하고, 위반 사항이 있으면 수정해줘:

$ARGUMENTS

## 검증 대상 파일 결정

- 인자가 파일 경로이면 해당 파일들을 검증
- 인자가 비어있으면 현재 브랜치에서 변경된 .kt 파일들을 자동 탐지 (git diff --name-only backend/dev -- '*.kt')

## 검증 → 수정 사이클 (순차 실행)

두 검증을 **순차적으로** 실행한다. 각 검증 후 위반이 있으면 즉시 수정한 뒤 다음 검증으로 넘어간다.

### 1단계: code-rules 검증 + 수정
1. code-rules-reviewer 에이전트 실행
   - `.claude/skills/code-implementation-rules/SKILL.md`를 읽고 모든 규칙에 대해 검증
   - 위반 사항만 보고 (칭찬/통과 사유 불필요)
2. 위반이 있으면:
   - code-implementation-rules 스킬을 Skill 도구로 로드
   - 위반 사항을 하나씩 수정
   - 수정 후 컴파일 확인 (./gradlew compileKotlin)
   - 테스트 통과 확인 (./gradlew test)
3. 위반이 0건이면 수정을 건너뛴다

### 2단계: clean-code 검증 + 수정
1. clean-code-reviewer 에이전트 실행 (1단계에서 수정된 코드 기준으로 검증)
   - `.claude/skills/clean-code/SKILL.md`를 읽고 모든 원칙에 대해 검증
   - 위반 사항만 보고 (칭찬/통과 사유 불필요)
2. 위반이 있으면:
   - clean-code 스킬을 Skill 도구로 로드
   - 위반 사항을 하나씩 수정
   - 수정 후 컴파일 확인 (./gradlew compileKotlin)
   - 테스트 통과 확인 (./gradlew test)
3. 위반이 0건이면 수정을 건너뛴다

## 결과 보고

최종 결과를 아래 형식으로 보고한다:

```
## 검증 결과 종합

| 출처 | 위반 | 내용 | 상태 |
|------|------|------|------|
| code-rules #N | 규칙명 | 위반 내용 요약 | 수정완료/미해결 |
| clean-code #N | 원칙명 | 위반 내용 요약 | 수정완료/미해결 |

1차 위반 N건 → 최종 N건 (N건 수정)
```

위반이 0건이면 "위반 사항 없음"으로 보고한다.

## 주의사항
- 두 에이전트는 반드시 **순차적으로** 실행할 것 (병렬 실행 금지)
- 1단계 수정이 완료된 후 2단계를 실행할 것
- 에이전트에게 검증 대상 파일 목록을 명확히 전달할 것
- 에이전트가 스킬 파일을 반드시 Read로 읽도록 프롬프트에 명시할 것
- 수정 시 반드시 스킬을 먼저 로드한 후 코드를 수정할 것
