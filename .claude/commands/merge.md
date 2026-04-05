현재 브랜치의 PR을 merge하고 정리한 뒤, 지정한 브랜치로 돌아가줘.

돌아갈 브랜치: $ARGUMENTS

돌아갈 브랜치가 비어있으면 기본값으로 backend/dev 브랜치를 사용한다.

## 1단계: PR merge
1. 현재 브랜치에 열린 PR이 있는지 gh pr view로 확인
2. PR이 이미 merged 상태이거나 closed 상태이면 이 단계를 건너뛰고 2단계로 진행
3. PR이 없으면 이 단계를 건너뛰고 2단계로 진행
4. PR이 open 상태이면 gh pr merge로 merge (--merge 옵션 사용)

## 2단계: 브랜치 이동
1. 현재 브랜치 이름을 기억해둘 것 (3단계에서 삭제용)
2. 지정한 브랜치(또는 backend/dev)로 checkout
3. git pull로 최신화

## 3단계: 작업 브랜치 삭제
1. merge된 작업 브랜치를 로컬에서 삭제 (git branch -d)
2. 리모트에서도 삭제 (git push origin --delete), 이미 삭제되었으면 무시

## 주의사항
- merge 실패 시 원인을 알려주고 작업 중단
- 브랜치 삭제 전에 현재 브랜치가 아닌지 확인 (이미 checkout한 후여야 함)
