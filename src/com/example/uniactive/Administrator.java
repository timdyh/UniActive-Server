package com.example.uniactive;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Administrator {

    private String name;

    public Administrator(String name) {
        this.name = name;
    }

    public boolean checkIfRegister(Statement statement) {
        try {
            String SQL = String.format("SELECT * FROM admin WHERE name ='%s'", name);
            ResultSet rs = statement.executeQuery(SQL);
            //已经注册过用户
            return !rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean register(Statement statement, String password) {
        try {
            int Sign2Register = statement.executeUpdate
                    (String.format("insert into admin values ('%s', '%s')",
                            name, password));
            //注册成功
            return Sign2Register == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean login(Statement statement, String password) {
        try {
            String SQL = "SELECT password FROM admin WHERE name =" + name;
            ResultSet rs = statement.executeQuery(SQL);
            if (rs.next()) {
                return rs.getString("password").equals(password);
            } else {
                return false; //不存在该账号
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean passActivity(Statement statement, int id) {
        try {
            String SQL = "UPDATE activity SET act_status = 1 WHERE id=" + id;
            int Sign2Pass = statement.executeUpdate(SQL);
            return Sign2Pass == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean denyActivity(Statement statement, int id) {
        try {
            String SQL = "UPDATE activity SET act_status = 2 WHERE id = " + id;
            int Sign2Deny = statement.executeUpdate(SQL);
            return Sign2Deny == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteComment(Statement statement, String email, int id) {
        try {
            String SQL = String.format("UPDATE participate SET " +
                    "score = null, comment = null, comment_time = null " +
                    "WHERE user='%s' AND act_id='%d'", email, id);
            int Sign2Delete = statement.executeUpdate(SQL);
            return Sign2Delete == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
