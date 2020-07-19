package com.example.uniactive;

import java.util.HashMap;

public enum Labels {
    BOYA,
    SPORT,
    E_SPORTS,
    MUSIC,
    OPERA,
    SCHOLAR,
    SCIENCE,
    FENGRUBEI,
    COMPETITION,
    LECTURE,
    SYMPOSIUM,
    MAJOR_SELECTION,
    EMPLOYMENT,
    HIGHER_SCHOOL,
    ABROAD,
    HEALTH,
    PARTY;

    private static final HashMap<String, Integer> LABEL_ID;

    static {
        LABEL_ID = new HashMap<>();
        LABEL_ID.put("博雅", BOYA.ordinal());
        LABEL_ID.put("体育", SPORT.ordinal());
        LABEL_ID.put("音乐", MUSIC.ordinal());
        LABEL_ID.put("影视", OPERA.ordinal());
        LABEL_ID.put("学术", SCHOLAR.ordinal());
        LABEL_ID.put("科技", SCIENCE.ordinal());
        LABEL_ID.put("冯如杯", FENGRUBEI.ordinal());
        LABEL_ID.put("竞赛", COMPETITION.ordinal());
        LABEL_ID.put("讲座", LECTURE.ordinal());
        LABEL_ID.put("座谈", SYMPOSIUM.ordinal());
        LABEL_ID.put("专业选择", MAJOR_SELECTION.ordinal());
        LABEL_ID.put("就业", EMPLOYMENT.ordinal());
        LABEL_ID.put("升学", HIGHER_SCHOOL.ordinal());
        LABEL_ID.put("出国", ABROAD.ordinal());
        LABEL_ID.put("电竞", E_SPORTS.ordinal());
        LABEL_ID.put("健康", HEALTH.ordinal());
        LABEL_ID.put("娱乐", PARTY.ordinal());
    }

    public static int getLabelId(String label) {
        if (!LABEL_ID.containsKey(label)) {
            return -1;
        }
        return LABEL_ID.get(label);
    }

}
