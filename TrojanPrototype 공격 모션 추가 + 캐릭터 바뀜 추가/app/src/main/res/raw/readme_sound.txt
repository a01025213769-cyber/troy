이 폴더의 음원은 tools/make_sounds.py 가 코드로 합성한 것입니다. (외부 음원 없음 - 저작권 걱정 없음)
다시 만들려면 프로젝트 폴더에서:  python tools/make_sounds.py

  효과음  sfx_swing  sfx_hit  sfx_heavy  sfx_guard  sfx_dodge
          sfx_ultimate  sfx_bow  sfx_win  sfx_lose  sfx_select
  배경음  bgm_title  bgm_story  bgm_battle  bgm_boss  bgm_ending

다른 음원으로 바꾸려면 같은 이름의 파일(.wav / .ogg / .mp3)을 넣으면 됩니다. (자바 코드 수정 불필요)
같은 이름에 확장자만 다른 파일이 두 개 있으면 빌드가 실패하니, 원래 .wav 파일은 지워 주세요.

중요: res 폴더 안의 파일 이름은 소문자 a-z, 숫자 0-9, 밑줄(_) 만 쓸 수 있습니다.
     대문자나 점(.)이 이름에 들어가면 빌드가 실패합니다. (예: README.txt 는 불가)
     파일이 없으면 소리만 나지 않고 게임은 정상 동작합니다.
