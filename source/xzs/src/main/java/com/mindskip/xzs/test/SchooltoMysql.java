package com.mindskip.xzs.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SchooltoMysql {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/xzs";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        // 500 所中国大学的名称
        String[] universities = {
                "北京大学", "清华大学", "浙江大学", "上海交通大学", "复旦大学",
                "南京大学", "中国科学技术大学", "华中科技大学", "武汉大学", "西安交通大学",
                "中山大学", "北京航空航天大学", "同济大学", "东南大学", "中国人民大学",
                "北京理工大学", "南开大学", "山东大学", "天津大学", "西北工业大学",
                "厦门大学", "中南大学", "吉林大学", "华东师范大学", "中国农业大学",
                "大连理工大学", "华南理工大学", "湖南大学", "重庆大学", "东北大学",
                "兰州大学", "苏州大学", "西南大学", "暨南大学", "郑州大学",
                "中国海洋大学", "西北农林科技大学", "北京化工大学", "北京交通大学", "北京科技大学",
                "北京邮电大学", "北京林业大学", "北京中医药大学", "中国传媒大学", "中央财经大学",
                "对外经济贸易大学", "中国政法大学", "华北电力大学", "东北师范大学", "华东理工大学",
                "南京航空航天大学", "南京理工大学", "中国矿业大学", "河海大学", "江南大学",
                "南京农业大学", "中国药科大学", "南京师范大学", "上海财经大学", "上海大学",
                "东华大学", "上海外国语大学", "华东政法大学", "浙江工业大学", "杭州电子科技大学",
                "宁波诺丁汉大学", "浙江师范大学", "杭州师范大学", "温州医科大学", "浙江理工大学",
                "浙江工商大学", "浙江中医药大学", "中国计量大学", "浙江农林大学", "温州大学",
                "浙江财经大学", "浙江科技学院", "浙江传媒学院", "浙江外国语学院", "浙江音乐学院",
                "浙江越秀外国语学院", "宁波大学", "宁波财经学院", "宁波工程学院", "浙江警察学院",
                "浙江水利水电学院", "浙江树人学院", "浙江万里学院", "浙江财经大学东方学院", "浙江工业大学之江学院", "长春理工大学"
                // ... 继续添加剩余的大学名称
        };
        // 插入到数据库中
        insertUniversities(universities);
    }

    private static void insertUniversities(String[] universities) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // 1. 获取数据库连接
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            // 2. 插入 SQL 语句
            String sql = "INSERT INTO school (school_name) VALUES (?)";
            pstmt = conn.prepareStatement(sql);

            // 3. 遍历大学列表并插入
            for (String university : universities) {
                pstmt.setString(1, university);
                pstmt.executeUpdate(); // 执行插入
            }

            System.out.println("成功导入" + universities.length + "中国所大学数据！");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 4. 关闭连接和语句
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
