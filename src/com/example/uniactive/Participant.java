package com.example.uniactive;

import com.example.util.MailUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;

public class Participant {

    private String email;

    private static final Object joinLock = new Object();

    public Participant(String email) {
        this.email = email;
    }

    /**
     * 检查用户名重复
     * @return true if email doesn't exist
     */
    public boolean checkEmailExists(Statement statement) {
        try {
            String SQL = String.format("SELECT * FROM users " +
                    "WHERE email = '%s'", email);
            ResultSet rs = statement.executeQuery(SQL);
            return !rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 注册
     * @return 1 if register successfully
     */
    public int register(Statement statement, String password,
                        int gender, String nickname,
                        String label1, String label2, String label3,
                        String label4, String label5) {
        try {
            int[] act_labels = {
                    Labels.getLabelId(label1),
                    Labels.getLabelId(label2),
                    Labels.getLabelId(label3),
                    Labels.getLabelId(label4),
                    Labels.getLabelId(label5)
            };
            double[] arr = new double[20];
            int cnt = 0;
            for (int e : act_labels) {
                if (e < 0) continue;
                arr[e] = 1;
                cnt++;
            }
            if (cnt > 0) {
                for (int i = 0; i < 20; i++) {
                    arr[i] /= Math.sqrt(cnt);
                }
            }
            JSONArray ja = new JSONArray();
            for (double d : arr) {
                ja.put(d);
            }
            int Sign2Register = statement.executeUpdate(String.format(
                    "insert into users " +
                    "(email, password, gender, nickname, label_vec) " +
                    "values ('%s', '%s', %d, '%s', '%s')",
                    email, password, gender, nickname, ja.toString()));
            //注册成功
            return Sign2Register == 1 ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 登录
     * @return 1 if login successfully
     */
    public JSONObject login(Connection connection, Statement statement,
                            String password) {
        JSONObject jo = new JSONObject();
        try {
            String SQL = String.format("SELECT * FROM users WHERE email = '%s'", email);
            ResultSet rs = statement.executeQuery(SQL);
            if (rs.next()) {
                if (rs.getString("password").equals(password)) {
                    jo.put("status", 1);
                    jo.put("nickname", rs.getString("nickname"));
                    jo.put("user_status", rs.getString("status"));
                    jo.put("gender", rs.getString("gender"));
                    jo.put("last_login", rs.getString("last_login"));
                    jo.put("img", rs.getString("img"));
                    statement.executeUpdate
                            (String.format("update users set " +
                                    "last_login=current_timestamp " +
                                    "where email='%s'", email));
                    JSONArray ja = new JSONArray();
                    rs = statement.executeQuery(String.format("select * from " +
                            "users, participate, activity " +
                            "where id=act_id and user='%s' " +
                            "and email=holder and start_time>current_timestamp",
                            email));
                    while (rs.next()) {
                        JSONObject tmp = new JSONObject();
                        Activity.getCount(connection.createStatement(), rs, tmp,
                                rs.getInt("act_id"));
                        ja.put(tmp);
                    }
                    jo.put("act_to_start", ja);
                    return jo;
                } else {
                    jo.put("status", 0);
                    return jo; //密码错误
                }
            } else {
                jo.put("status", 0);
                return jo; //不存在该账号
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jo.put("status", -1);
        return jo;
    }

    /**
     * 修改资料
     * @return 1 if modify successfully
     */
    public int modifyInfo(Statement statement, String newNickname,
                          String img, int gender) {
        try {
            String sql = String.format("update users " +
                    "set nickname='%s', gender=%d, img='%s' " +
                    "where email='%s'", newNickname, gender, img, email);
            int status = statement.executeUpdate(sql);
            return status == 1 ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 查询已参加的活动
     */
    public JSONArray queryJoinedActs(Connection connection, Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            String SQL = String.format("SELECT * FROM participate, activity, users " +
                    "WHERE email=holder and user='%s' and act_id=id " +
                    "order by start_time", email);
            ResultSet rs = statement.executeQuery(SQL);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                Activity.getCount(connection.createStatement(),
                        rs, jo, rs.getInt("act_id"));
                jo.put("join_time", rs.getString("join_time"));
                jo.put("score", rs.getInt("score"));
                jo.put("comment", rs.getString("comment"));
                ja.put(jo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return ja;
    }

    private static void collect(Connection connection, JSONArray ja,
                                HashSet<Integer> hashSet, ResultSet rs)
            throws SQLException {
        while (rs.next()) {
            int id = rs.getInt("id");
            if (!hashSet.contains(id)) {
                hashSet.add(id);
                JSONObject jo = new JSONObject();
                Activity.getCount(connection.createStatement(), rs, jo,
                        rs.getInt("id"));
                ja.put(jo);
            }
        }
    }

    /**
     * 按关键字查找
     */
    public JSONArray searchByKeyword(Connection connection, Statement statement,
                                     String str) {
        try {
            str = str.replace("'", "\\'");
            str = str.replace("%", "\\%");
            str = str.replace("_", "\\_");
            JSONArray ja = new JSONArray();
            String[] arr = {"name", "introduction", "label1", "label2",
                    "label3", "label4", "label5"};
            HashSet<Integer> hashSet = new HashSet<>();
            for (String s : arr) {
                String sql = "select * from activity, users " +
                        "where email=holder and start_time>current_timestamp " +
                        "and act_status=1 and " + s + " like '%" + str + "%'";
                ResultSet rs = statement.executeQuery(sql);
                collect(connection, ja, hashSet, rs);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONArray searchByLabel(Connection connection, Statement statement,
                                   String label) {
        try {
            JSONArray ja = new JSONArray();
            String[] arr = {"label1", "label2", "label3", "label4", "label5"};
            HashSet<Integer> hashSet = new HashSet<>();
            for (String s : arr) {
                ResultSet rs = statement.executeQuery(String.format(
                        "select * from activity, users where holder=email " +
                                "and start_time>current_timestamp " +
                                "and act_status=1 and %s='%s'", s, label));
                collect(connection, ja, hashSet, rs);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查是否已参加该活动
     * @return 1 if haven't joined activity id
     */
    public int checkJoin(Statement statement, int id) {
        try {
            String SQL = String.format("SELECT act_id FROM participate " +
                    "WHERE user='%s' AND act_id=%d", email, id);
            ResultSet rs = statement.executeQuery(SQL);
            return rs.next() ? 0 : 1; // 用户没有参与到这个活动之中，可以报名。
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 不能参加自己发布的活动
     * @return 1 if not the holder of activity id
     */
    public int checkHold(Statement statement, int id) {
        try {
            String sql = "select holder from activity where id=" + id;
            ResultSet rs = statement.executeQuery(sql);
            rs.next();
            return email.equals(rs.getString("holder")) ? 0 : 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 参加活动
     * @return 1 if join successfully
     */
    public int joinActivity(Statement statement, int id) {
        synchronized (joinLock) {
            try {
                ResultSet rs = statement.executeQuery(
                        "select max_num>count(user) as cnt " +
                                "from activity, participate " +
                                "where act_id=id and id=" + id);
                rs.next();
                Object o = rs.getObject("cnt");
                if (o != null) {
                    int over = rs.getInt("cnt");
                    if (over == 0) {
                        // 满员
                        return 2;
                    }
                }
                rs = statement.executeQuery(String.format(
                        "select id from activity, participate " +
                        "where user='%s' and id=act_id " +
                        "and start_time<(select end_time from activity " +
                        "where id=%d) " +
                        "and end_time>(select start_time from activity " +
                        "where id=%d)", email, id, id));
                if (rs.next()) {
                    // 时间冲突
                    return 0;
                }
                statement.executeUpdate(
                        String.format("insert into participate " +
                        "(user, act_id, join_time) values " +
                        "('%s', %d, current_timestamp)", email, id));
                //报名成功
                return 1;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }
    }

    /**
     * 退出活动
     * @return 1 if quit successfully
     */
    public int quitActivity(Statement statement, int id) {
        try {
            String SQL = String.format("DELETE FROM participate " +
                    "WHERE user='%s' AND act_id =%d", email, id);
            int Sign2Quit = statement.executeUpdate(SQL);
            //退出活动成功
            return Sign2Quit == 1 ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 评论
     * @return 1 if comment successfully
     */
    public int addComment(Statement statement, int id,
                              String comment, int score) {
        try {
            String SQL = String.format("UPDATE participate SET score=%d, " +
                    "comment='%s', comment_time=current_timestamp " +
                    "WHERE user='%s' AND act_id=%d", score, comment, email, id);
            int Sign2Add = statement.executeUpdate(SQL);
            //添加评论成功
            return Sign2Add == 1 ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int delComment(Statement statement, int id) {
        try {
            String sql = String.format("update participate " +
                    "set comment=null, comment_time=null, score=null " +
                    "where user='%s' and act_id=%d", email, id);
            int status = statement.executeUpdate(sql);
            return status == 1 ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 提醒
     * @return activities to remind
     */
    public JSONArray remindActivity(Connection connection, Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            String sql = String.format("SELECT * FROM participate, activity, users " +
                    "WHERE id=act_id and email=holder AND user='%s' " +
                    "AND start_time between " +
                    "current_timestamp and '%s'", email, comingTime());
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                Activity.getCount(connection.createStatement(), rs, jo,
                        rs.getInt("act_id"));
                ja.put(jo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return ja;
    }

    /**
     * 30分钟后
     */
    private static Timestamp comingTime() {
        Timestamp CurrentTime  = new Timestamp(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        c.setTime(CurrentTime);
        c.add(Calendar.MINUTE,30);
        return new Timestamp(c.getTimeInMillis());
    }

    public int addDiscuss(Statement statement, int act_id, String content) {
        content = content.replace("'", "\\'");
        try {
            String sql = String.format("insert into discuss " +
                    "(publisher, act_id, content) values ('%s', %d, '%s')",
                    email, act_id, content);
            statement.executeUpdate(sql);
            sql = "select last_insert_id()";
            ResultSet rs = statement.executeQuery(sql);
            int lastId = 0;
            if (rs.next()) {
                lastId = rs.getInt(1);
            }
            return lastId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int delDiscuss(Statement statement, int disc_id) {
        try {
            String sql = "delete from discuss where disc_id=" + disc_id;
            statement.executeUpdate(sql);
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int modifyDiscuss(Statement statement, int disc_id, String content) {
        content = content.replace("'", "\\'");
        try {
            statement.executeUpdate(String.format("update discuss " +
                    "set content='%s', post_time=current_timestamp " +
                    "where disc_id=%d", content, disc_id));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int addFavorite(Statement statement, int act_id) {
        try {
            statement.executeUpdate(String.format("insert into favorite " +
                    "set fav_user='%s', fav_act_id=%d", email, act_id));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int cancelFavorite(Statement statement, int act_id) {
        try {
            statement.executeUpdate(String.format("delete from favorite " +
                    "where fav_user='%s' and fav_act_id=%d", email, act_id));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONArray queryFavorite(Connection connection, Statement statement) {
        JSONArray ja = new JSONArray();
        try {
            ResultSet rs = statement.executeQuery(
                    String.format("select * from favorite, activity, users " +
                            "where holder=email and fav_user='%s' " +
                            "and fav_act_id=id", email));
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                Activity.getCount(connection.createStatement(), rs, jo,
                        rs.getInt("id"));
                ja.put(jo);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int checkFav(Statement statement, int act_id) {
        try {
            ResultSet rs = statement.executeQuery(String.format(
                    "select * from favorite " +
                            "where fav_user='%s' and fav_act_id=%d",
                    email, act_id));
            return rs.next() ? 1 : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 注销说明：<br/>
     * 1、不能有参与的未结束的活动<br/>
     * 2、不能有发布的未结束的活动<br/>
     * 3、注销后，所有活动参与记录、收藏、活动评价清除<br/>
     * 4、发布的已结束的活动仍然保留，但holder字段变为“账号已注销”<br/>
     * 5、讨论区发言保留，且发言人昵称不变
     * @return 1 if logoff successfully
     */
    public int logoff(Connection connection, Statement statement, String password) {
        try {
            ResultSet rs = statement.executeQuery(String.format(
                    "select password from users where email='%s'", email));
            rs.next();
            if ( !password.equals(rs.getString("password"))) {
                return 2;
            }
            rs = statement.executeQuery(
                    String.format("select * from activity where holder='%s' " +
                            "and end_time>current_timestamp", email));
            if (rs.next()) {
                return 0;
            }
            rs = statement.executeQuery(
                    String.format("select * from participate, activity " +
                            "where act_id=id and user='%s' " +
                            "and end_time>current_timestamp", email));
            if (rs.next()) {
                return 0;
            }
            connection.setAutoCommit(false);
            statement.executeUpdate(
                    String.format("update activity set holder='%%%%%%' " +
                            "where holder='%s'", email));
            statement.executeUpdate(
                    String.format("delete from participate where user='%s'",
                            email));
            statement.executeUpdate(String.format("delete from favorite " +
                    "where fav_user='%s'", email));
            statement.executeUpdate(
                    String.format("delete from users where email='%s'", email));
            connection.commit();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public int feedback(Statement statement, String opinion) {
        opinion = opinion.replace("'", "\\'");
        try {
            statement.executeUpdate(String.format("insert into feedback " +
                    "(provider, opinion, time) " +
                    "values ('%s', '%s', current_timestamp)", email, opinion));
            ResultSet rs = statement.executeQuery("select last_insert_id()");
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int delFeedback(Statement statement, int fbid) {
        try {
            statement.executeUpdate("delete from feedback where fbid=" + fbid);
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int modifyFeedback(Statement statement, int fbid, String opinion) {
        opinion = opinion.replace("'", "\\'");
        try {
            statement.executeUpdate(String.format("update feedback " +
                    "set opinion='%s', time=current_timestamp where fbid=%d",
                    opinion, fbid));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONArray queryMyFeedback(Statement statement) {
        try {
            ResultSet rs = statement.executeQuery(
                    String.format("select * from feedback where provider='%s'",
                            email));
            JSONArray ja = new JSONArray();
            while (rs.next()) {
                JSONObject jo = new JSONObject();
                jo.put("fbid", rs.getInt("fbid"));
                jo.put("provider", rs.getString("provider"));
                jo.put("opinion", rs.getString("opinion"));
                jo.put("time", rs.getTimestamp("time").getTime() - 28800000);
                ja.put(jo);
            }
            return ja;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int modifyPassword(Statement statement,
                              String oldPassword, String newPassword) {
        try {
            ResultSet rs = statement.executeQuery(
                    String.format("select password from users " +
                            "where email='%s'", email));
            rs.next();
            String password = rs.getString("password");
            if (!oldPassword.equals(password)) {
                return 0;
            }
            statement.executeUpdate(String.format("update users " +
                    "set password='%s' where email='%s'", newPassword, email));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 重置密码
     * @return 1 -- 成功；<br>
     *     3 -- 未发送验证码或验证码过期；<br>
     *         2 -- 验证码错误；<br>
     *             -1 -- 系统错误
     */
    public int resetPassword(Statement statement,
                             String verifyCode, String newPassword) {
        int status = MailUtils.checkVerifyCode(email, verifyCode);
        if (status != 1) {
            return status;
        }
        try {
            ResultSet rs = statement.executeQuery(String.format(
                    "select * from users where email='%s'", email));
            if (!rs.next()) {
                return 4;
            }
            statement.executeUpdate(String.format("update users " +
                    "set password='%s' where email='%s'", newPassword, email));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
