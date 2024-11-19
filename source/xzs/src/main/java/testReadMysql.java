import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class testReadMysql {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root"; // 数据库用户名
    private static final String PASSWORD = "123456"; // 数据库密码




    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 定义要插入的用户数据
        String userName = "admin"; // 用户名
        String password = "123456"; // 密码
        String realName = "管理员"; // 真实姓名
        String school = "长春理工大学"; // 学校
        String stuNo = null; // 学号
        int userLevel = 3; // 用户级别
        boolean deleted = false; // 是否被删除
        int status = 1; // 状态
        int role = 3; // 角色
        String userUuid = String.valueOf(UUID.randomUUID()); // 用户UUID
        long lastActiveTime = System.currentTimeMillis(); // 最后活动时间
        long createTime = System.currentTimeMillis(); // 创建时间
        long modifyTime = System.currentTimeMillis(); // 修改时间

        String sql = "INSERT INTO t_user (user_name, password, real_name, school, stuNo, user_level, deleted, status, role, user_uuid, last_active_time, create_time, modify_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // 1. 加载驱动程序
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. 获得数据库的连接
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // 3. 创建 PreparedStatement
            pstmt = conn.prepareStatement(sql);

            // 获取当前日期和时间
            java.util.Date utilDate = new java.util.Date();

            // 转换为 java.sql.Date
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

            // 4. 设置参数
            pstmt.setString(1, userName);
            pstmt.setString(2, password);
            pstmt.setString(3, realName);
            pstmt.setString(4, school);
            pstmt.setString(5, stuNo);
            pstmt.setInt(6, userLevel);
            pstmt.setBoolean(7, deleted);
            pstmt.setInt(8, status);
            pstmt.setInt(9, role);
            pstmt.setString(10, userUuid);
            // 假设你有一个 PreparedStatement 对象 pstmt
            pstmt.setDate(11, sqlDate); // 最后活动时间
            pstmt.setDate(12, sqlDate); // 创建时间
            pstmt.setDate(13, sqlDate); // 修改时间
            // 5. 执行插入操作
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("成功插入 " + rowsAffected + " 行数据。");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 6. 关闭资源
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 用户信息类，用于存储查询结果
    static class UserInfo {
        private Long userId;
        private String className;

        public UserInfo(Long userId, String className) {
            this.userId = userId;
            this.className = className;
        }

        public Long getUserId() {
            return userId;
        }

        public String getClassName() {
            return className;
        }
    }
}
