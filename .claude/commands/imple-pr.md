지정한 plan 문서를 기반으로 브랜치 생성 → 구현 → PR 생성까지 자동 수행해줘.

plan 문서 경로: $ARGUMENTS

## 1단계: plan 문서 읽기
1. 지정된 plan 문서를 Read로 읽어 구현 내용을 파악
2. plan 문서에서 기능명(feature name)을 추출

## 2단계: 브랜치 생성
1. 현재 브랜치에서 새 브랜치를 생성
2. 브랜치명 규칙: `backend/feat/{feature-name}` (plan 문서의 기능명을 kebab-case로)
3. git checkout -b {브랜치명}

## 3단계: 구현
1. code-implementer 에이전트를 사용하여 plan 문서의 내용을 구현
2. 구현 전에 반드시:
   - 관련 기존 코드를 읽고 패턴을 파악할 것
   - clean-code 스킬과 code-implementation-rules 스킬을 참조할 것
   - 구현 완료 후 컴파일 확인할 것
3. plan 문서의 구현 순서를 따를 것

## 4단계: 커밋 + 푸시
1. git status로 변경사항 확인
2. git diff로 변경 내용 파악
3. 적절한 커밋 메시지를 작성하여 커밋
   - .env, credentials 등 민감 파일은 제외
   - 커밋 메시지는 이 레포지토리의 기존 커밋 메시지 스타일을 따를 것
4. git push -u origin {현재브랜치}

## 5단계: PR 생성
1. 대상 브랜치는 backend/dev
2. PR 제목은 70자 이내로 간결하게 작성
3. PR 본문에 변경사항 요약 + plan 문서 경로를 포함
4. gh pr create로 PR 생성
5. 생성된 PR URL을 반환

## 주의사항
- plan 문서가 존재하지 않으면 작업 중단하고 안내
- 구현 중 컴파일 에러 발생 시 수정 후 재시도
- 전체 테스트 통과 확인 후 커밋
