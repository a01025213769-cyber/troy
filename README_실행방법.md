# 트로이 전쟁 - 저장 및 챕터 선택 통합본

이 프로젝트는 `TrojanPrototype 공격 모션 추가 + 캐릭터 바뀜 추가` 프로젝트의 Gradle 설정을 유지하고, ZIP 안의 최신 `main/` 소스 전체를 `app/src/main/`에 적용한 통합본입니다.

## 포함된 기능
- 저장 슬롯 3개 및 이어하기
- 챕터 선택 및 진행도에 따른 해금
- 기존 공격 모션 및 캐릭터 변경 관련 리소스/코드(최신 main 소스 기준)

## Android Studio 실행
1. 이 폴더(`TroyWar_SaveChapter_Integrated`)를 Android Studio에서 Open 합니다.
2. Android Studio가 SDK 경로를 설정하도록 기다립니다. 원본 ZIP의 `local.properties`는 다른 PC의 SDK 경로를 가리켜 제거했습니다.
3. Gradle Sync가 끝나면 실행 기기(에뮬레이터 또는 휴대폰)를 선택합니다.
4. 상단 Run(초록색 삼각형)을 누릅니다.

주의: 이 환경에서는 Android SDK/Gradle 빌드 실행을 수행하지 않았으므로, 실제 빌드 성공 여부는 Android Studio에서 Gradle Sync 및 Run으로 확인해야 합니다.
