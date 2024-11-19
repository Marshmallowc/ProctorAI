package com.mindskip.xzs.controller.student;

import com.mindskip.xzs.service.StudentFileUploadService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentFileUploadController {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    @Autowired
    private StudentFileUploadService studentFileUploadService;
    @PersistenceContext
    private EntityManager entityManager; // 使用 EntityManager 直接操作数据库
    @Autowired
    private JdbcTemplate jdbcTemplate;



    @PostMapping("/import")
    public ResponseEntity<String> importStudents(@RequestParam("file") MultipartFile file) {
        System.out.println("I'm in /import");
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        System.out.println("next to try // 解析文件内容并提取必要数据");
        try {
            // 解析文件内容并提取必要数据
            System.out.println("即将开始解析文件内容");
            List<Student> students = parseFile(file);
            System.out.println("解析文件内容并提取必要数据");
            System.out.println(students);

            // 遍历学生列表并输出信息
            for (Student student : students) {
                System.out.println("用户名: " + student.getUserName());
                System.out.println("密码: " + student.getPassword());
                System.out.println("真实姓名: " + student.getRealName());
                System.out.println("学校: " + student.getSchool());
                System.out.println("学号: " + student.getStuNo());
                System.out.println("班级: " + student.getClassNo());
                System.out.println("------------------------");
            }

            System.out.println("学生数量");
            System.out.println(students.size());

            System.out.println(students.get(0).getRealName());



            // 批量插入到数据库
            saveAll(students);

            // 批量将学号处理为班级2205214
            dealWithStuNo();

            return ResponseEntity.ok("学生信息导入成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("导入失败: " + e.getMessage());
        }
    }

    // 遍历xzs数据库中的t_user数据表，将t_user中的stuNo的最后两位数去掉，剩下的内容填入class_name中
    public void dealWithStuNo() {
        // SQL 查询语句，获取 t_user 表中的 stuNo 和 id
        String selectQuery = "SELECT id, stuNo FROM t_user";
        // SQL 更新语句，更新 class_name 字段
        String updateQuery = "UPDATE t_user SET class_name = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
             ResultSet rs = selectStmt.executeQuery()) {

            // 遍历查询结果
            while (rs.next()) {
                int id = rs.getInt("id"); // 获取用户ID
                String stuNo = rs.getString("stuNo"); // 获取学号

                // 去掉 stuNo 的最后两位数字
                if (stuNo != null && stuNo.length() > 2) {
                    String updatedClassName = stuNo.substring(0, stuNo.length() - 2);

                    // 设置更新语句的参数
                    updateStmt.setString(1, updatedClassName); // class_name
                    updateStmt.setInt(2, id); // id

                    // 执行更新操作
                    updateStmt.executeUpdate();
                    System.out.println("Updated ID: " + id + " with class_name: " + updatedClassName);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveAll(List<Student> students) throws SQLException {

        String sql = "INSERT INTO t_user (user_name, password, real_name, school, stuNo, user_level, deleted, status, role, user_uuid, last_active_time, create_time, modify_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        for (Student student : students) {
            String stuNo = student.getStuNo();
            String classRoom = "";
            if (stuNo != null && stuNo.length() > 2) {
                classRoom = stuNo.substring(0, stuNo.length() - 2);
                System.out.println("提取的班级: " + classRoom);
            } else {
                System.out.println("学号无效: " + stuNo);
            }

            // 连接数据库
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                // 检测class表是否存在，若不存在则创建一个
                checkAndCreateClassTable();

                // 检查班级是否存在
                ResultSet rs = stmt.executeQuery("SELECT class_name FROM class WHERE class_name = '" + classRoom + "'");
                boolean classExists = rs.next(); // 获取班级存在性
                System.out.println("检查班级是否存在");
                rs.close(); // 关闭班级的 ResultSet

                // 检查学校是否存在
                ResultSet sc = stmt.executeQuery("SELECT school FROM t_user WHERE school = '" + student.getSchool() + "'");
                boolean schoolExists = sc.next(); // 获取学校存在性
                System.out.println("检查学校是否存在");
                sc.close(); // 关闭学校的 ResultSet

                // 检查学号是否存在
                ResultSet sno = stmt.executeQuery("SELECT stuNo FROM t_user WHERE stuNo = '" + student.getStuNo() + "'");
                boolean studentExists = sno.next(); // 获取学号存在性
                System.out.println("检查学号是否存在");
                sno.close(); // 关闭学号的 ResultSet

                if (schoolExists && studentExists) {
                    // 该学生已存在
                    System.out.println("学生已经存在");
                    continue; // 跳过此学生的处理
                }

                if (!classExists) {
                    Connection connection = null;

                    // 如果班级不存在，则添加
                    String insertSQL = "INSERT INTO class (class_name, class_no, college_no, major_no, grade_no) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement pstmt = null;

                    try {
                        // 获取数据库连接
                        connection = DriverManager.getConnection(URL, USER, PASSWORD);

                        pstmt = connection.prepareStatement(insertSQL);
                        pstmt.setString(1, classRoom); // class_name
                        pstmt.setString(2, classRoom.substring(4)); // class_no
                        pstmt.setString(3, classRoom.substring(0, 4)); // college_no
                        pstmt.setString(4, classRoom.substring(0, 5)); // major_no
                        pstmt.setString(5, classRoom.substring(0, 2)); // grade_no
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        if (pstmt != null) {
                            pstmt.close();
                        }
                    }
                    System.out.println("班级 " + classRoom + " 已添加到数据库.");
                } else {
                    System.out.println("班级 " + classRoom + " 已存在.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }



            jdbcTemplate.update(sql,
                    student.getUserName(),
                    student.getPassword(),
                    student.getRealName(),
                    student.getSchool(),
                    student.getStuNo(),
                    1,
                    0x00,
                    1,
                    1,
                    student.getUser_uuid(),
                    student.getLastActiveTime(),
                    new Date(),
                    new Date());
        }


    }

    public void checkAndCreateClassTable() {
        // SQL 语句：检查 class 表是否存在
        String checkTableExistsQuery = "SHOW TABLES LIKE 'class'";

        // SQL 语句：创建 class 表
        String createTableQuery = "CREATE TABLE class (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "class_name VARCHAR(255), " +
                "class_no VARCHAR(255), " +
                "school_no VARCHAR(255), " +
                "college_no VARCHAR(255), " +
                "major_no VARCHAR(255), " +
                "grade_no VARCHAR(255), " +
                "PRIMARY KEY (id))";

        // 检查表是否存在
        try {
            String tableName = jdbcTemplate.queryForObject(checkTableExistsQuery, String.class);

            if (tableName == null) {
                // 表不存在，创建表
                jdbcTemplate.execute(createTableQuery);
                System.out.println("class 表已创建。");
            } else {
                System.out.println("class 表已存在，无需创建。");
            }
        } catch (Exception e) {
            // 如果捕获异常，说明表不存在，需要创建
            jdbcTemplate.execute(createTableQuery);
            System.out.println("class 表不存在，已创建。");
        }
    }


    private Workbook getWorkbook(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream()); // 处理Excel 2007及以上版本
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(file.getInputStream()); // 处理Excel 2003版本
        } else {
            throw new IllegalArgumentException("Invalid file format. Please upload .xls or .xlsx file.");
        }
    }


    private List<Student> parseFile(MultipartFile file) throws IOException {
        List<Student> students = new ArrayList<>();
        try (Workbook workbook = getWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            // 跳过第一行（列名）
            for (int rowIndex = 1; rowIndex < sheet.getLastRowNum(); rowIndex++) {
                System.out.println("这里是getLastRowNum()" + sheet.getLastRowNum());
                Row row = sheet.getRow(rowIndex);
                String cellVal = getCellValue(row.getCell(0));
                if(cellVal.equals(""))  continue;
                if (row != null) {
                    Student student = new Student();
                    student.setSchool(getCellValue(row.getCell(0))); // 假设学校在第四列
                    student.setStuNo(getCellValue(row.getCell(1))); // 假设学号在第五列
                    student.setUserName(getCellValue(row.getCell(2))); // 假设用户名在第一列
                    student.setPassword(getCellValue(row.getCell(3))); // 假设密码在第二列
                    student.setRealName(getCellValue(row.getCell(4))); // 假设真实姓名在第三列
                    student.setClassNo(getCellValue(row.getCell(5))); // 假设班级在第六列
                    student.setUser_uuid(UUID.randomUUID().toString());
                    student.setCreateTime(new Date());
                    student.setLastActiveTime(new Date());
                    students.add(student);
                }
            }
        }
        return students;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 检查是否为整数
                if (DateUtil.isCellDateFormatted(cell)) {
                    return String.valueOf(cell.getDateCellValue());
                } else {
                    // 将数字格式化为字符串
                    return String.format("%.0f", cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }



    private class Student {
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getSchool() {
            return school;
        }

        public void setSchool(String school) {
            this.school = school;
        }

        public String getStuNo() {
            return stuNo;
        }

        public void setStuNo(String stuNo) {
            this.stuNo = stuNo;
        }

        public String getClassNo() {
            return classNo;
        }

        public void setClassNo(String classNo) {
            this.classNo = classNo;
        }

        private String userName;
        private String password;
        private String realName;
        private String school;
        private String stuNo;
        private String classNo;

        public String getUser_uuid() {
            return user_uuid;
        }

        public void setUser_uuid(String user_uuid) {
            this.user_uuid = user_uuid;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getLastActiveTime() {
            return lastActiveTime;
        }

        public void setLastActiveTime(Date lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
        }

        private String user_uuid;
        private Date createTime;
        private Date lastActiveTime;

    }
}


