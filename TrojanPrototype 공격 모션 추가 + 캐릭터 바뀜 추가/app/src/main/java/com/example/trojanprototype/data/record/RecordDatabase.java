package com.example.trojanprototype.data.record;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 전투 기록 데이터베이스 (Room · SQLite).
 *
 * 앱 전체에서 하나만 만들어 씁니다(싱글턴). 파일은 기기 안 앱 전용 폴더에 저장되어
 * 앱을 껐다 켜도 남고, 앱을 지우면 함께 지워집니다.
 * 표 구조를 바꾸면 version 을 올려야 합니다.
 */
@Database(entities = {BattleRecord.class}, version = 1, exportSchema = false)
public abstract class RecordDatabase extends RoomDatabase {

    public abstract BattleRecordDao dao();

    private static volatile RecordDatabase instance;

    public static RecordDatabase get(Context ctx) {
        if (instance == null) {
            synchronized (RecordDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(ctx.getApplicationContext(),
                            RecordDatabase.class, "troy_records.db").build();
                }
            }
        }
        return instance;
    }
}
