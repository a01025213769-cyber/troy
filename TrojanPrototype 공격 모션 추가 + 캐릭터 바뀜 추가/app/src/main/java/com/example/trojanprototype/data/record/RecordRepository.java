package com.example.trojanprototype.data.record;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 게임과 데이터베이스 사이의 창구.
 *
 * ★ 왜 따로 스레드를 쓰나 ★
 * 데이터베이스 읽기·쓰기는 디스크를 건드려 느릴 수 있어서,
 * 안드로이드(Room)는 화면을 그리는 스레드에서 DB 를 쓰면 오류를 냅니다.
 * 그래서 작업 전용 스레드 하나(Executor)에 맡기고, 결과는 "스냅샷"으로 받아 화면에 그립니다.
 */
public class RecordRepository {

    /** 화면에 그릴 때 쓰는 읽기 전용 묶음 */
    public static final class Snapshot {
        public final List<BattleRecordDao.StageStat> stages;
        public final List<BattleRecord> recent;
        public final int total;

        Snapshot(List<BattleRecordDao.StageStat> stages, List<BattleRecord> recent, int total) {
            this.stages = stages;
            this.recent = recent;
            this.total = total;
        }
    }

    private final BattleRecordDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Snapshot snapshot = null;

    public RecordRepository(Context ctx) {
        this.dao = RecordDatabase.get(ctx).dao();
    }

    /** 전투 결과 한 판을 저장합니다 (백그라운드). */
    public void add(final BattleRecord r) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dao.insert(r);
                } catch (Throwable ignored) {
                    // 저장에 실패해도 게임은 계속됩니다.
                }
            }
        });
    }

    /** 전적 화면을 열 때 다시 읽어 옵니다. 다 읽으면 snapshot() 이 새 값으로 바뀝니다. */
    public void refresh() {
        snapshot = null;
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    snapshot = new Snapshot(dao.stageStats(), dao.recent(7), dao.count());
                } catch (Throwable e) {
                    snapshot = new Snapshot(new ArrayList<BattleRecordDao.StageStat>(),
                            new ArrayList<BattleRecord>(), 0);
                }
            }
        });
    }

    /** 아직 읽는 중이면 null */
    public Snapshot snapshot() {
        return snapshot;
    }
}
