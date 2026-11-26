package com.example.trojanprototype.data.record;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 전투 한 판의 기록 — Room 데이터베이스의 표(battle_record) 한 줄.
 *
 * 담당: 유장원 (시스템)
 *
 * @Entity 를 붙이면 Room 이 이 클래스를 보고 SQLite 표를 자동으로 만듭니다.
 * (필드 하나 = 열 하나)
 */
@Entity(tableName = "battle_record")
public class BattleRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 스테이지 번호 (GameData.STAGES 의 인덱스) */
    public int stage;
    /** 난이도 번호 (0 쉬움 / 1 보통 / 2 어려움) */
    public int difficulty;
    /** 이겼으면 1, 졌으면 0 (SQL 의 SUM 으로 승리 수를 세려고 int 로 둡니다) */
    public int win;
    /** 남은 체력 % */
    public int hpPercent;
    /** 걸린 시간(초) */
    public int seconds;
    /** 최대 연속 타격 */
    public int bestCombo;
    /** 사용 캐릭터 이름 */
    public String hero;
    /** 기록한 시각 (밀리초) */
    public long playedAt;
}
