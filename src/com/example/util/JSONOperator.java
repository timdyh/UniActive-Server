package com.example.util;

import com.example.uniactive.Activity;
import com.example.uniactive.Holder;
import com.example.uniactive.Participant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public final class JSONOperator {

    private final JSONObject in;
    private final Statement statement;
    private final Connection connection;
    private JSONObject out;

    public JSONOperator(JSONObject in, Connection connection)
            throws SQLException {
        this.in = in;
        this.connection = connection;
        this.statement = connection.createStatement();
        this.out = new JSONObject();
    }

    public String execute() {
        String request = in.getString("request").toUpperCase();
        switch (RequestType.valueOf(request)) {
            case LOGIN:
                return login();
            case REGISTER:
                return register();
            case MODIFY_INFO:
                return modifyInfo();
            case QUERY_JOINED:
                return queryJoined();
            case JOIN:
                return join();
            case MODIFY:
                return modify();
            case QUIT:
                return quit();
            case COMMENT:
                return addComment();
            case USER_DEL_COMMENT:
                return userDelComment();
            case HOLD:
                return hold();
            case CANCEL:
                return cancel();
            case QUERY_HELD:
                return queryHeld();
            /*case ADMIN_LOGIN:
                return adminLogin();
            case ADMIN_REGISTER:
                return adminRegister();
            case ADMIN_DEL_COMMENT:
                return adminDelComment();
            case PASS:
                return pass();
            case DENY:
                return deny();*/
            case GET_DETAIL:
                return getDetail();
            case IF_USER_JOIN:
                return checkJoin();
            case QUERY_REMIND:
                return remindActivity();
            case SEARCH:
                return searchByKeyword();
            case SEARCH_LABEL:
                return searchByLabel();
            case ADD_DISCUSS:
                return addDiscuss();
            case DEL_DISCUSS:
                return delDiscuss();
            case QUERY_DISCUSS:
                return queryDiscuss();
            case MODIFY_DISCUSS:
                return modifyDiscuss();
            case ALL_ACTIVITY:
                return getAllActivity();
            case GET_ACT_COMMENTS:
                return getActComments();
            case ADD_FAV:
                return addFav();
            case CANCEL_FAV:
                return cancelFav();
            case QUERY_FAV:
                return queryFav();
            case CHECK_FAV:
                return checkFav();
            case LOGOFF:
                return logoff();
            case SEND_VERIFY_CODE:
                return sendVerifyCode();
            case CHANGEPSW:
                return modifyPassword();
            case RESET_PASSWORD:
                return resetPassword();
            case FEEDBACK:
                return feedback();
            case MODIFY_FEEDBACK:
                return modifyFeedback();
            case DEL_FEEDBACK:
                return delFeedback();
            case QUERY_FEEDBACK:
                return queryMyFeedback();
            default:
                return out.toString();
        }
    }

    private String getResult(JSONArray ja) {
        if (ja == null) {
            out.put("status", -1);
        } else {
            out.put("status", 1);
            out.put("result", ja);
        }
        return out.toString();
    }

    private String login() {
        String email = in.getString("email");
        String password = in.getString("password");
        Participant participant = new Participant(email);
        return participant.login(connection, statement, password).toString();
    }

    private String register() {
        String email = in.getString("email");
        String password = in.getString("password");
        int gender = in.getInt("gender");
        String nickname = in.getString("nickname");
        String verifyCode = in.getString("verify_code");
        String label1 = in.getString("label1");
        String label2 = in.getString("label2");
        String label3 = in.getString("label3");
        String label4 = in.getString("label4");
        String label5 = in.getString("label5");
        Participant participant = new Participant(email);
        int verify = MailUtils.checkVerifyCode(email, verifyCode);
        if (verify != 1) {
            out.put("status", verify);
            return out.toString();
        }
        boolean status = participant.checkEmailExists(statement);
        if (status) {
            out.put("status", participant.register(statement, password,
                            gender, nickname,
                    label1, label2, label3, label4, label5));
        } else {
            out.put("status", 0);
        }
        return out.toString();
    }

    private String modifyInfo() {
        String email = in.getString("email");
        String newNickname = in.getString("nickname");
        int gender = in.getInt("gender");
        String img = in.getString("img");
        Participant participant = new Participant(email);
        int status = participant.modifyInfo(statement, newNickname, img, gender);
        out.put("status", status);
        return out.toString();
    }

    private String queryJoined() {
        String email = in.getString("email");
        Participant participant = new Participant(email);
        JSONArray ja = participant.queryJoinedActs(connection, statement);
        return getResult(ja);
    }

    private String checkJoin() {
        // TODO: 目前只有用户点击详情时需要调用
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        Activity activity = new Activity(act_id);
        activity.getDetail(connection, statement, email); // 目的在于更新用户标签
        int status = participant.checkJoin(statement, act_id);
        int if_fav = participant.checkFav(statement, act_id);
        out.put("status", status);
        if (if_fav < 0) {
            out.put("status", -1);
        } else {
            out.put("if_fav", if_fav);
        }
        return out.toString();
    }

    private String join() {
        /*status = participant.checkHold(statement, act_id);
        if (status != 1) {
            out.put("status", status);
            return out.toString();
        }*/
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.checkJoin(statement, act_id);
        if (status != 1) {
            out.put("status", status);
            return out.toString();
        }
        status = participant.joinActivity(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String quit() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.quitActivity(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String addComment() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        String com = in.getString("comment");
        int score = in.getInt("score");
        Participant participant = new Participant(email);
        int status = participant.addComment(statement, act_id, com, score);
        out.put("status", status);
        return out.toString();
    }

    /**
     * 发布者获取活动的所有评价
     */
    private String getActComments() {
        int act_id = in.getInt("act_id");
        Activity activity = new Activity(act_id);
        JSONArray ja = activity.getActComments(statement);
        return getResult(ja);
    }

    private String userDelComment() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.delComment(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String hold() {
        String email = in.getString("email");
        String name = in.getString("name");
        Timestamp start_time = new Timestamp(in.getLong("start_time"));
        Timestamp end_time = new Timestamp(in.getLong("end_time"));
        int max_num = in.getInt("max_num");
        String introduction = in.getString("introduction");
        String label1 = in.getString("label1");
        String label2 = in.getString("label2");
        String label3 = in.getString("label3");
        String img1 = in.getString("img1");
        /*String img2 = in.getString("img2");
        String img3 = in.getString("img3");*/
        String place = "";
        if (in.has("place")) {
            place = in.getString("place");
        }
        Holder holder = new Holder(email);
        int act_id = holder.createActivity(
                statement, name, start_time, end_time,
                max_num, introduction, place,
                label1, label2, label3, img1);
        if (act_id > 0) {
            out.put("status", 1);
            out.put("act_id", act_id);
        } else {
            out.put("status", act_id);
        }
        return out.toString();
    }

    private String modify() {
        int act_id = in.getInt("act_id");
        String name = in.getString("name");
        Timestamp start_time = new Timestamp(in.getLong("start_time"));
        Timestamp end_time = new Timestamp(in.getLong("end_time"));
        int max_num = in.getInt("max_num");
        String introduction = in.getString("introduction");
        String label1 = in.getString("label1");
        String label2 = in.getString("label2");
        String label3 = in.getString("label3");
        String img1 = in.getString("img1");
        String place = "";
        if (in.has("place")) {
            place = in.getString("place");
        }
        Holder holder = new Holder("");
        int res = holder.modifyActivity(statement, name, act_id,
                start_time, end_time, max_num, introduction, place,
                label1, label2, label3, img1);
        out.put("status", res);
        return out.toString();
    }

    private String cancel() {
        int act_id = in.getInt("act_id");
        Holder holder = new Holder("");
        int status = holder.deleteActivity(connection, statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String queryHeld() {
        String email = in.getString("email");
        Holder holder = new Holder(email);
        JSONArray ja = holder.queryHeldActs(connection, statement);
        return getResult(ja);
    }

    private String remindActivity() {
        String email = in.getString("email");
        Participant participant = new Participant(email);
        JSONArray ja = participant.remindActivity(connection, statement);
        return getResult(ja);
    }

    private String getAllActivity() {
        String email = in.getString("email");
        Activity act = new Activity(-1);
        JSONArray ja = act.getAllActivity(connection, statement, email);
        return getResult(ja);
    }

    private String getDetail() {
        int id = in.getInt("act_id");
        String email = in.getString("email");
        Activity act = new Activity(id);
        return act.getDetail(connection, statement, email).toString();
    }

    private String searchByKeyword() {
        String keyword = in.getString("keyword");
        Participant participant = new Participant("");
        JSONArray ja = participant.searchByKeyword(connection, statement, keyword);
        return getResult(ja);
    }

    private String searchByLabel() {
        String label = in.getString("label");
        Participant participant = new Participant("");
        JSONArray ja = participant.searchByLabel(connection, statement, label);
        return getResult(ja);
    }

    private String addDiscuss() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        String content = in.getString("content");
        Participant participant = new Participant(email);
        int status = participant.addDiscuss(statement, act_id, content);
        if (status < 0) {
            out.put("status", status);
        } else {
            out.put("status", 1);
            out.put("disc_id", status);
        }
        return out.toString();
    }

    private String delDiscuss() {
        int disc_id = in.getInt("disc_id");
        Participant participant = new Participant("");
        int status = participant.delDiscuss(statement, disc_id);
        out.put("status", status);
        return out.toString();
    }

    private String queryDiscuss() {
        int act_id = in.getInt("act_id");
        Activity activity = new Activity(act_id);
        JSONArray ja = activity.queryDiscuss(statement);
        return getResult(ja);
    }

    private String modifyDiscuss() {
        int disc_id = in.getInt("disc_id");
        String content = in.getString("content");
        Participant participant = new Participant("");
        int status = participant.modifyDiscuss(statement, disc_id, content);
        out.put("status", status);
        return out.toString();
    }

    private String addFav() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.addFavorite(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String cancelFav() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.cancelFavorite(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String queryFav() {
        String email = in.getString("email");
        Participant participant = new Participant(email);
        JSONArray ja = participant.queryFavorite(connection, statement);
        return getResult(ja);
    }

    private String checkFav() {
        String email = in.getString("email");
        int act_id = in.getInt("act_id");
        Participant participant = new Participant(email);
        int status = participant.checkFav(statement, act_id);
        out.put("status", status);
        return out.toString();
    }

    private String logoff() {
        String email = in.getString("email");
        String password = in.getString("password");
        Participant participant = new Participant(email);
        int status = participant.logoff(connection, statement, password);
        out.put("status", status);
        return out.toString();
    }

    private String sendVerifyCode() {
        String email = in.getString("email");
        int status = MailUtils.sendMail(email);
        out.put("status", status);
        return out.toString();
    }

    private String modifyPassword() {
        String email = in.getString("email");
        String oldPassword = in.getString("old_psw");
        String newPassword = in.getString("new_psw");
        Participant participant = new Participant(email);
        int status = participant.modifyPassword(statement, oldPassword, newPassword);
        out.put("status", status);
        return out.toString();
    }

    private String resetPassword() {
        String email = in.getString("email");
        String verifyCode = in.getString("verify_code");
        String newPassword = in.getString("new_psw");
        Participant participant = new Participant(email);
        int status = participant.resetPassword(statement, verifyCode, newPassword);
        out.put("status", status);
        return out.toString();
    }

    private String feedback() {
        String provider = in.getString("provider");
        String opinion = in.getString("opinion");
        Participant participant = new Participant(provider);
        int status = participant.feedback(statement, opinion);
        if (status < 0) {
            out.put("status", status);
        } else {
            out.put("status", 1);
            out.put("fbid", status);
        }
        return out.toString();
    }

    private String modifyFeedback() {
        int fbid = in.getInt("fbid");
        String opinion = in.getString("opinion");
        Participant participant = new Participant("");
        int status = participant.modifyFeedback(statement, fbid, opinion);
        out.put("status", status);
        return out.toString();
    }

    private String delFeedback() {
        int fbid = in.getInt("fbid");
        Participant participant = new Participant("");
        int status = participant.delFeedback(statement, fbid);
        out.put("status", status);
        return out.toString();
    }

    private String queryMyFeedback() {
        String provider = in.getString("provider");
        Participant participant = new Participant(provider);
        JSONArray ja = participant.queryMyFeedback(statement);
        return getResult(ja);
    }

}
