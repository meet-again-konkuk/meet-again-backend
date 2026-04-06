현재 브랜치의 변경사항을 커밋, 푸시하고 PR을 merge한 뒤 지정한 브랜치로 돌아가줘.

돌아갈 브랜치: $ARGUMENTS

돌아갈 브랜치가 비어있으면 기본값으로 backend/dev 브랜치를 사용한다.

## 1단계: 커밋
1. git status로 변경사항 확인 (staged, unstaged, untracked 모두)
2. git diff로 변경 내용 파악
3. git log로 최근 커밋 메시지 스타일 확인
4. 변경사항이 있으면 적절한 커밋 메시지를 작성하여 커밋
   - .env, credentials 등 민감 파일은 제외
   - 커밋 메시지는 이 레포지토리의 기존 커밋 메시지 스타일을 따를 것
5. 변경사항이 없으면 커밋을 건너뛰고 2단계로 진행

## 2단계: 푸시
1. 리모트 브랜치가 있는지 확인
2. git push -u origin {현재브랜치} 로 푸시
3. 푸시할 내용이 없으면 이 단계를 건너뛰고 3단계로 진행

## 3단계: PR merge
1. 현재 브랜치에 열린 PR이 있는지 gh pr view로 확인
2. PR이 이미 merged 상태이거나 closed 상태이면 이 단계를 건너뛰고 4단계로 진행
3. PR이 없으면 이 단계를 건너뛰고 4단계로 진행
4. PR이 open 상태이면 gh pr merge로 merge (--merge 옵션 사용)

## 4단계: 브랜치 이동
1. 현재 브랜치 이름을 기억해둘 것 (5단계에서 삭제용)
2. 지정한 브랜치(또는 backend/dev)로 checkout
3. git pull로 최신화

## 5단계: 작업 브랜치 삭제
1. merge된 작업 브랜치를 로컬에서 삭제 (git branch -d)
2. 리모트에서도 삭제 (git push origin --delete), 이미 삭제되었으면 무시

## 주의사항
- merge 실패 시 원인을 알려주고 작업 중단
- 브랜치 삭제 전에 현재 브랜치가 아닌지 확인 (이미 checkout한 후여야 함)
