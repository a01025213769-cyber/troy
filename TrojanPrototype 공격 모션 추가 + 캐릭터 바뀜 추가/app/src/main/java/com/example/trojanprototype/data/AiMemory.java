package com.example.trojanprototype.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.trojanprototype.ai.PlayerModel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * 적 AI 가 배운 플레이어 습관을 저장합니다.
 *
 * 담당: 유장원 (AI)
 *
 * PlayerModel.State 를 Gson 으로 JSON 문자열 하나로 바꿔 SharedPreferences 에 넣습니다.
 *   예) {"guardRate":0.62,"dodgeRate":0.18,"aggression":0.47,"samples":14}
 * 그래서 앱을 껐다 켜도 AI 가 "지난번에 이 플레이어는 자주 막았다"를 기억합니다.
 */
public class AiMemory {

    private static final String PREFS = "troy_ai";
    private static final String KEY = "player_model";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public AiMemory(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 저장된 습관을 모델에 불러옵니다. 저장이 없으면 출발값 그대로 둡니다. */
    public void load(PlayerModel model) {
        String json = sp.getString(KEY, null);
        if (json == null) return;
        try {
            model.apply(gson.fromJson(json, PlayerModel.State.class));
        } catch (JsonSyntaxException ignored) {
            // 깨진 저장은 무시하고 처음부터 배웁니다.
        }
    }

    public void save(PlayerModel model) {
        sp.edit().putString(KEY, gson.toJson(model.toState())).apply();
    }

    /** 배운 내용을 지웁니다 (전적 화면의 "AI 학습 초기화") */
    public void clear(PlayerModel model) {
        model.reset();
        sp.edit().remove(KEY).apply();
    }
}
