#!/bin/sh
# 난이도 밸런스 시뮬레이터 (Git Bash / macOS / Linux)
#
#   sh tools/balance_sim/run.sh          # 칸마다 150판
#   sh tools/balance_sim/run.sh 300      # 칸마다 300판
#
# 실제 게임 코드(BattleWorld · Fighter · EnemyBrain · GameData)를 그대로 가져와
# 안드로이드 없이 컴퓨터에서 컴파일하고, 사람처럼 반응이 늦는 가상 플레이어로
# 스테이지 x 난이도 x 실력 조합마다 전투를 반복해 승률을 출력합니다.
# 안드로이드 전용 클래스(RectF, Bitmap, 소리, 터치)는 stub/ 의 간단한 대역으로 바꿔 끼웁니다.
#
# GameData.java 의 숫자를 고친 뒤 다시 돌려 보면 난이도 변화를 바로 확인할 수 있습니다.
# 필요한 것: JDK (Android Studio 에 들어 있는 jbr 을 기본으로 씁니다. JAVA_HOME 으로 바꿀 수 있음)

HERE="$(cd "$(dirname "$0")" && pwd)"
if command -v cygpath >/dev/null 2>&1; then HERE="$(cygpath -m "$HERE")"; fi
SRC="$HERE/../../app/src/main/java"
N="${1:-150}"
WORK="$HERE/.work"

# javac 찾기: JAVA_HOME → Android Studio 의 jbr → PATH 순서
J=""
for cand in "$JAVA_HOME/bin" "/c/Program Files/Android/Android Studio/jbr/bin" \
            "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin"; do
    if [ -x "$cand/javac" ] || [ -x "$cand/javac.exe" ]; then J="$cand"; break; fi
done
if [ -z "$J" ]; then J="$(dirname "$(command -v javac)")"; fi

rm -rf "$WORK"
mkdir -p "$WORK/build" "$WORK/src"
cp -r "$HERE/stub/"* "$WORK/src/"
P=com/example/trojanprototype
mkdir -p "$WORK/src/$P/battle" "$WORK/src/$P/ai" "$WORK/src/$P/data" "$WORK/src/$P/ui" "$WORK/src/$P/util"
cp "$SRC/$P/battle/BattleWorld.java" "$SRC/$P/battle/Fighter.java" "$WORK/src/$P/battle/"
cp "$SRC/$P/ai/EnemyBrain.java" "$WORK/src/$P/ai/"
cp "$SRC/$P/data/GameData.java" "$WORK/src/$P/data/"
cp "$SRC/$P/ui/Theme.java" "$WORK/src/$P/ui/"
cp "$SRC/$P/util/SpriteSet.java" "$WORK/src/$P/util/"
# 폴더 이름에 한글이 있어도 되도록 상대 경로로 컴파일합니다.
cd "$WORK/src" || exit 1
find . -name '*.java' > ../files.txt
"$J/javac" -encoding UTF-8 -nowarn -d ../build @../files.txt || exit 1
cd "$WORK" || exit 1
"$J/java" -cp build sim.Sim "$N"
cd "$HERE" && rm -rf "$WORK"
