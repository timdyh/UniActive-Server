package com.example.uniactive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;

public class Activity {

    private int id;
    /*private String name;
    private String holder;
    private Timestamp apply_time;
    private Timestamp start_time;
    private Timestamp end_time;
    private int maxnum;
    private String introduction;
    private int act_status;
    private String label1;
    private String label2;
    private String label3;
    private String label4;
    private String label5;
    private String img1;
    private String img2;
    private String img3;*/

    public Activity(int id) {
        this.id = id;
    }

    private static void act2json(ResultSet rs, JSONObject jo)
            throws SQLException {
        jo.put("act_id", rs.getInt("id"));
        jo.put("name", rs.getString("name"));
        jo.put("holder", rs.getString("nickname"));
        jo.put("holder_email", rs.getString("holder"));
        jo.put("apply_time", rs.getTimestamp("apply_time").getTime() - 28800000);
        jo.put("start_time", rs.getTimestamp("start_time").getTime() - 28800000);
        jo.put("end_time", rs.getTimestamp("end_time").getTime() - 28800000);
        jo.put("max_num", rs.getString("max_num"));
        jo.put("introduction", rs.getString("introduction"));
        jo.put("act_status", rs.getInt("act_status"));
        jo.put("label1", rs.getString("label1"));
        jo.put("label2", rs.getString("label2"));
        jo.put("label3", rs.getString("label3"));
        jo.put("label4", rs.getString("label4"));
        jo.put("label5", rs.getString("label5"));
        jo.put("img1", rs.getString("img1"));
        jo.put("img2", rs.getString("img2"));
        jo.put("img3", rs.getString("img3"));
        jo.put("img", rs.getString("img"));
        jo.put("reject", rs.getString("reject"));
        jo.put("place", rs.getString("place"));
    }

    static void getCount(Statement statement, ResultSet rs,
                         JSONObject jo, int act_id) throws SQLException {
        act2json(rs, jo);
        ResultSet tmp = statement.executeQuery(
                "select count(user) as cnt from participate " +
                        "where act_id=" + act_id);
        tmp.next();
        jo.put("count", tmp.getInt("cnt"));
    }

    public JSONObject getDetail(Connection connection, Statement statement,
                                String email) {
        JSONObject jo = new JSONObject();
        try {
            String sql = "SELECT * FROM activity, users " +
                    "WHERE email=holder and id=" + id;
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                getCount(connection.createStatement(), rs, jo, id);
                jo.put("status", 1);
                jo.put("if_user_join", false);
                if (email.length() == 0) return jo;
                int[] act_labels = {
                        Labels.getLabelId(rs.getString("label5")),
                        Labels.getLabelId(rs.getString("label4")),
                        Labels.getLabelId(rs.getString("label3")),
                        Labels.getLabelId(rs.getString("label2")),
                        Labels.getLabelId(rs.getString("label1"))
                };
                rs = statement.executeQuery(String.format(
                        "select * from participate " +
                                "where user='%s' and act_id=%d", email, id));
                jo.put("if_user_join", rs.next());
                rs = statement.executeQuery(String.format("select label_vec " +
                        "from users where email='%s'", email));
                if (!rs.next()) return jo;
                double[] interests = new double[20];
                // 用户兴趣
                JSONArray ja = new JSONArray(rs.getString("label_vec"));
                for (int i = 0; i < ja.length(); i++) {
                    interests[i] = ja.getDouble(i);
                }
                for (int e : act_labels) {
                    if (e < 0) continue;
                    euclidRefresh(interests, e, 0.8);
                }
                ja = new JSONArray(interests);
                statement.executeUpdate(String.format("update users " +
                        "set label_vec='%s' where email='%s'",
                        ja.toString(), email));
            } else {
                jo.put("status", -1); //活动id不存在
            }
            return jo;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONArray getActComments(Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            String SQL = String.format("select * from participate, users " +
                    "where user=email and score>0 and act_id=%d", id);
            ResultSet rs = statement.executeQuery(SQL);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                jo.put("user", rs.getString("user"));
                jo.put("score", rs.getInt("score"));
                jo.put("comment", rs.getString("comment"));
                jo.put("comment_time", rs.getString("comment_time"));
                jo.put("img", rs.getString("img"));
                ja.put(jo);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONArray queryDiscuss(Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            String sql = "select * from discuss, users " +
                    "where email=publisher and act_id=" + id;
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                jo.put("disc_id", rs.getInt("disc_id"));
                jo.put("publisher", rs.getString("publisher"));
                jo.put("act_id", rs.getInt("act_id"));
                jo.put("post_time", rs.getString("post_time"));
                jo.put("content", rs.getString("content"));
                jo.put("img", rs.getString("img"));
                ja.put(jo);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class MyComparator implements Comparator<JSONObject> {

        private final double[] labelVec = new double[20];

        private MyComparator(JSONArray userLabelVec) {
            for (int i = 0; i < userLabelVec.length(); i++) {
                labelVec[i] = userLabelVec.getDouble(i);
            }
        }

        private static double[] jo2arr(JSONObject jo) {
            double[] arr = new double[20];
            int[] act_labels = {
                    Labels.getLabelId(jo.getString("label1")),
                    Labels.getLabelId(jo.getString("label2")),
                    Labels.getLabelId(jo.getString("label3")),
                    Labels.getLabelId(jo.getString("label4")),
                    Labels.getLabelId(jo.getString("label5"))
            };
            int cnt = 0;
            for (int e : act_labels) {
                if (e < 0) continue;
                cnt++;
                arr[e] = 1;
            }
            if (cnt > 0) {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] /= Math.sqrt(cnt);
                }
            }
            return arr;
        }

        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            double d1 = euclidDistance2(jo2arr(o1), labelVec);
            double d2 = euclidDistance2(jo2arr(o2), labelVec);
            return Double.compare(d1, d2);
        }
    }

    public JSONArray getAllActivity(Connection connection, Statement statement,
                                    String email) {
        JSONArray ja = new JSONArray();
        ArrayList<JSONObject> arr = new ArrayList<>();
        try {
            String sql = "SELECT * FROM activity, users " +
                    "where act_status=1 and start_time>current_timestamp " +
                    "and email=holder order by start_time";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                getCount(connection.createStatement(), rs, jo, rs.getInt("id"));
                arr.add(jo);
            }
            if (email.length() != 0) {
                rs = statement.executeQuery(String.format(
                        "select label_vec from users where email='%s'", email));
                if (rs.next()) {
                    JSONArray tmp = new JSONArray(rs.getString("label_vec"));
                    arr.sort(new MyComparator(tmp));
                }
            }
            for (JSONObject jo : arr) {
                ja.put(jo);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*private static void manhattanRefresh(double[] interest,
                                         int newInterest, double rate) {
        for (int i = 0; i < interest.length; i++) {
            interest[i] *= rate;
        }
        interest[newInterest] += 1 - rate;
    }

    private static double manhattanDistance(double[] a1, double[] a2) {
        double d = 0;
        for (int i = 0; i < a1.length; i++) {
            d += Math.abs(a1[i] - a2[i]);
        }
        return d;
    }*/

    private static void euclidRefresh(double[] interest,
                                      int newInterest, double rate) {
        for (int i = 0; i < interest.length; i++) {
            interest[i] *= rate;
        }
        interest[newInterest] *= interest[newInterest];
        interest[newInterest] += 1 - rate * rate;
        interest[newInterest] = Math.sqrt(interest[newInterest]);
    }

    private static double euclidDistance2(double[] a1, double[] a2) {
        double d = 0;
        for (int i = 0; i < a1.length; i++) {
            d += (a1[i] - a2[i]) * (a1[i] - a2[i]);
        }
        return d;
    }

}
