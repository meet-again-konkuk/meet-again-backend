다음 기능을 구현하고, 구현된 코드를 검증하여 위반 사항이 있으면 수정해줘:

$ARGUMENTS

## 1단계: 인자 분석

- 인자가 plan 문서 경로(`docs/plan/...`로 시작)면 Read로 읽어 기능명(feature name) 추출
- 자유 텍스트 인자면 그 자체에서 기능명을 kebab-case로 추론

## 2단계: 브랜치 생성

1. 현재 브랜치 확인 (`git branch --show-current`)
2. 현재 브랜치가 `develop` 또는 `main`이면 새 브랜치 생성:
   - 브랜치명: `feat/{feature-name}` (`backend/` 접두사 없음)
   - `git checkout -b feat/{feature-name}`
3. 이미 작업 브랜치(`feat/`, `refactor/`, `fix/` 시작)에 있으면 그대로 사용 (새로 안 만듦)

## 3단계: 구현

1. 관련 기존 코드를 읽고 패턴을 파악할 것
2. clean-code 스킬과 code-implementation-rules 스킬을 참조할 것
3. 구현 완료 후 컴파일 확인 (./gradlew compileKotlin)
4. 테스트 통과 확인 (./gradlew test)

## 4단계: 검증 대상 파일 결정

- 현재 브랜치에서 변경된 .kt 파일들을 자동 탐지 (git diff --name-only develop -- '*.kt')

## 5단계: code-rules 검증 + 수정

1. `.claude/skills/code-implementation-rules/SKILL.md`를 읽고 모든 규칙에 대해 검증
2. 위반 사항만 보고 (칭찬/통과 사유 불필요)
3. 위반이 있으면:
   - code-implementation-rules 스킬을 Skill 도구로 로드
   - 위반 사항을 하나씩 수정
   - 수정 후 컴파일 확인 (./gradlew compileKotlin)
   - 테스트 통과 확인 (./gradlew test)
4. 위반이 0건이면 수정을 건너뛴다

## 6단계: clean-code 검증 + 수정

1. `.claude/skills/clean-code/SKILL.md`를 읽고 모든 원칙에 대해 검증
2. 위반 사항만 보고 (칭찬/통과 사유 불필요)
3. 위반이 있으면:
   - clean-code 스킬을 Skill 도구로 로드
   - 위반 사항을 하나씩 수정
   - 수정 후 컴파일 확인 (./gradlew compileKotlin)
   - 테스트 통과 확인 (./gradlew test)
4. 위반이 0건이면 수정을 건너뛴다

## 결과 보고

최종 결과를 아래 형식으로 보고한다:

```
## 검증 결과 종합

브랜치: feat/{feature-name}

| 출처 | 위반 | 내용 | 상태 |
|------|------|------|------|
| code-rules #N | 규칙명 | 위반 내용 요약 | 수정완료/미해결 |
| clean-code #N | 원칙명 | 위반 내용 요약 | 수정완료/미해결 |

1차 위반 N건 → 최종 N건 (N건 수정)
```

위반이 0건이면 "위반 사항 없음"으로 보고한다.

## 주의사항
- **브랜치 생성은 develop/main에서만 자동 수행**. 작업 브랜치 위에 있을 땐 그대로 사용 (실수로 덮어쓰지 않음)
- 구현 완료 후 검증 단계로 넘어갈 것 (구현과 검증을 병렬 실행하지 않음)
- 검증 시 스킬 파일을 반드시 Read로 읽을 것
- 수정 시 반드시 스킬을 먼저 로드한 후 코드를 수정할 것
- 테스트 코드 작성과 PR 생성은 본 커맨드 범위 외 (`/test`, `/pr` 별도 실행)