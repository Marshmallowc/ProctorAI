package library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDatabaseConnection {

    // JDBC 驱动名和数据库 URL
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // 数据库的用户名和密码
    static final String USER = "root";  // 根据你的数据库用户名设置
    static final String PASS = "123456";  // 根据你的数据库密码设置

    public static void main(String[] args) {
        Connection conn = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);

            // 打开链接
            System.out.println("连接到数据库...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // 检查是否连接成功
            if (conn != null) {
                System.out.println("数据库连接成功！");
            }

        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 最后关闭资源
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
