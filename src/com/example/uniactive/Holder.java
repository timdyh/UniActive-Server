package com.example.uniactive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class Holder {

    private String email;

    public Holder(String email) {
        this.email = email;
    }

    public int createActivity(Statement statement, String name,
                              Timestamp start_time, Timestamp end_time,
                              int max_num, String introduction, String place,
                              String label1, String label2, String label3,
                              String img1) {
        try {
            introduction = introduction.replace("'", "\\'");
            name = name.replace("'", "\\'");
            statement.executeUpdate(String.format("insert into activity " +
                            "(name, holder, start_time, end_time, max_num, " +
                            "introduction, label1, label2, label3, img1, place) " +
                            "values ('%s', '%s', '%s', '%s', %d, " +
                            "'%s', '%s', '%s', '%s', '%s', '%s')",
                            name, email, start_time, end_time, max_num,
                    introduction, label1, label2, label3, img1, place));
            ResultSet tmp = statement.executeQuery("select last_insert_id()");
            tmp.next();
            return tmp.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 查询发起的活动
     */
    public JSONArray queryHeldActs(Connection connection, Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            String SQL = String.format("SELECT * FROM activity, users " +
                    "WHERE holder=email and holder='%s' " +
                    "order by start_time", email);
            ResultSet rs = statement.executeQuery(SQL);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                Activity.getCount(connection.createStatement(), rs, jo,
                        rs.getInt("id"));
                ja.put(jo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return ja;
    }

    /**
     * 发布者修改活动
     */
    public int modifyActivity(Statement statement, String name, int act_id,
                              Timestamp start_time, Timestamp end_time,
                              int max_num, String introduction, String place,
                              String label1, String label2, String label3,
                              String img1) {
        try {
            introduction = introduction.replace("'", "\\'");
            name = name.replace("'", "\\'");
            statement.executeUpdate(String.format("update activity " +
                            "set name='%s', start_time='%s', act_status=0, " +
                            "end_time='%s', max_num='%s', " +
                            "introduction='%s', place='%s', " +
                            "label1='%s', label2='%s', label3='%s', img1='%s' " +
                            "where id=%d",
                    name, start_time, end_time, max_num, introduction, place,
                    label1, label2, label3, img1, act_id));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int deleteActivity(Connection conn, Statement statement, int id) {
        try {
            conn.setAutoCommit(false);
            statement.executeUpdate(
                    "DELETE FROM participate WHERE act_id=" + id);
            statement.executeUpdate(
                    "DELETE FROM activity WHERE id=" + id);
            conn.commit();
            return 1;
        } catch(SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            return -1;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
