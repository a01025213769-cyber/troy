package com.example.trojanprototype.data.record;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * 전투 기록 표를 읽고 쓰는 SQL 모음 (DAO = Data Access Object).
 *
 * 메서드 위에 SQL 만 적어 두면 Room 이 컴파일할 때 실제 코드를 만들어 줍니다.
 * SQL 이 틀리면 앱 실행 전에 빌드 단계에서 오류로 알려 줍니다.
 */
@Dao
public interface BattleRecordDao {

    @Insert
    void insert(BattleRecord record);

    /** 최근 기록 n 개 (최신순) */
    @Query("SELECT * FROM battle_record ORDER BY playedAt DESC LIMIT :n")
    List<BattleRecord> recent(int n);

    /** 스테이지별 통계 — 도전 횟수, 승리 수, 가장 빨리 이긴 시간, 가장 많이 남긴 체력 */
    @Query("SELECT stage, COUNT(*) AS plays, SUM(win) AS wins, "
            + "MIN(CASE WHEN win = 1 THEN seconds END) AS bestSeconds, "
            + "MAX(CASE WHEN win = 1 THEN hpPercent END) AS bestHp "
            + "FROM battle_record GROUP BY stage ORDER BY stage")
    List<StageStat> stageStats();

    @Query("SELECT COUNT(*) FROM battle_record")
    int count();

    /** 스테이지별 통계 한 줄 (SQL 결과를 담는 그릇) */
    class StageStat {
        public int stage;
        public int plays;
        public int wins;
        /** 이긴 적이 없으면 null */
        public Integer bestSeconds;
        public Integer bestHp;
    }
}
