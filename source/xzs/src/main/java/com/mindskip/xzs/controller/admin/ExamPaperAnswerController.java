package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.ExamPaperAnswer;
import com.mindskip.xzs.domain.Subject;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.service.*;
import com.mindskip.xzs.utility.DateTimeUtil;
import com.mindskip.xzs.utility.ExamUtil;
import com.mindskip.xzs.utility.PageInfoHelper;
import com.mindskip.xzs.viewmodel.admin.user.UserResponseVM;
import com.mindskip.xzs.viewmodel.student.exampaper.ExamPaperAnswerPageResponseVM;
import com.mindskip.xzs.viewmodel.admin.paper.ExamPaperAnswerPageRequestVM;
import com.github.pagehelper.PageInfo;

import java.awt.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController("AdminExamPaperAnswerController")
@RequestMapping(value = "/api/admin/examPaperAnswer")
public class ExamPaperAnswerController extends BaseApiController {

    private final ExamPaperAnswerService examPaperAnswerService;
    private final SubjectService subjectService;
    private final UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    @Autowired
    public ExamPaperAnswerController(ExamPaperAnswerService examPaperAnswerService, SubjectService subjectService, UserService userService) {
        this.examPaperAnswerService = examPaperAnswerService;
        this.subjectService = subjectService;
        this.userService = userService;
    }


    @RequestMapping(value = "/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<ExamPaperAnswerPageResponseVM>> pageJudgeList(@RequestBody ExamPaperAnswerPageRequestVM model) {
        System.out.println("这里是ExamPaperAnswerController.java 中的/page，下面将打印model");
        PageInfo<ExamPaperAnswer> pageInfo = examPaperAnswerService.adminPage(model);
        PageInfo<ExamPaperAnswerPageResponseVM> page = PageInfoHelper.copyMap(pageInfo, e -> {
            ExamPaperAnswerPageResponseVM vm = modelMapper.map(e, ExamPaperAnswerPageResponseVM.class);
            Subject subject = subjectService.selectById(vm.getSubjectId());
            vm.setDoTime(ExamUtil.secondToVM(e.getDoTime()));
            vm.setSystemScore(ExamUtil.scoreToVM(e.getSystemScore()));
            vm.setUserScore(ExamUtil.scoreToVM(e.getUserScore()));
            vm.setPaperScore(ExamUtil.scoreToVM(e.getPaperScore()));
            vm.setSubjectName(subject.getName());
            vm.setCreateTime(DateTimeUtil.dateFormat(e.getCreateTime()));
            User user = userService.selectById(e.getCreateUser());
            vm.setUserName(user.getUserName());
            vm.setSchool(user.getSchool());
            vm.setStuNo(user.getStuNo());
            vm.setRealName(user.getRealName());
            return vm;
        });

// 打印排序前的列表
        System.out.println("排序前的列表：");
        for (ExamPaperAnswerPageResponseVM vm : page.getList()) {
            System.out.printf("姓名: %s, 得分: %s, 创建日期: %s, 试卷名称: %s, 学校: %s, 学号: %s%n",
                    vm.getRealName(), vm.getUserScore(), vm.getCreateTime(),
                    vm.getSubjectName(), vm.getSchool(), vm.getStuNo());
        }

        // 对 page 进行排序
        Collections.sort(page.getList(), Comparator
                .comparing(ExamPaperAnswerPageResponseVM::getSchool, Comparator.nullsLast(Comparator.naturalOrder())) // 学校升序
                .thenComparing(ExamPaperAnswerPageResponseVM::getSubjectName, Comparator.nullsLast(Comparator.naturalOrder())) // 试卷名称升序
                .thenComparing(ExamPaperAnswerPageResponseVM::getStuNo, Comparator.nullsLast(Comparator.naturalOrder())) // 学号升序
                .thenComparing(
                        vm -> {
                            // 将 UserScore 转换为 Integer
                            try {
                                return Integer.parseInt(vm.getUserScore());
                            } catch (NumberFormatException e) {
                                return Integer.MIN_VALUE; // 或其他适当的值
                            }
                        }, Comparator.nullsLast(Comparator.reverseOrder()))
        );


// 打印排序后的列表
        System.out.println("排序后的列表：");
        for (ExamPaperAnswerPageResponseVM vm : page.getList()) {
            System.out.printf("姓名: %s, 得分: %s, 创建日期: %s, 试卷名称: %s, 学校: %s, 学号: %s%n",
                    vm.getRealName(), vm.getUserScore(), vm.getCreateTime(),
                    vm.getSubjectName(), vm.getSchool(), vm.getStuNo());
        }

        return RestResponse.ok(page);
    }

    // 新增：获取专业号和学号列表
    @GetMapping("/getMajors")
    public RestResponse<MajorResponse> getMajors() {
        // 查询数据库中的学号
        String sql = "SELECT class_name FROM class where class_name is not null";  // 替换为实际的表名和字段
        String sql1 = "SELECT DISTINCT paper_name FROM t_exam_paper_answer";
        List<String> studentNos = jdbcTemplate.queryForList(sql, String.class);
        List<String> paperName = jdbcTemplate.queryForList(sql1, String.class);

        // 分别提取年级号、专业号、班级号和学号
        List<String> gradeNos = studentNos.stream()
                .map(studentNo -> studentNo.substring(0, 2)) // 提取年级号
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> majorCodes = studentNos.stream()
                .map(studentNo -> studentNo.substring(0, 5))// 提取专业号
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> classNos = studentNos.stream()
                .map(studentNo -> studentNo.substring(4)) // 提取班级号
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> stuNos = studentNos.stream()
                .map(studentNo -> studentNo.substring(7)) // 提取学号
                .collect(Collectors.toList());

        // 构建响应对象
        MajorResponse response = new MajorResponse();
        response.setGradeNos(gradeNos);
        response.setMajorCodes(majorCodes);
        response.setClassNos(classNos);
        response.setStuNos(stuNos);
        response.setPaperName(paperName);

        System.out.println("---------------------");
        for(String cls: response.paperName) {
            System.out.println("试卷名称" + cls);
        }
        System.out.println("---------------------");

        System.out.println("---------------------");
        for(String cls: response.classNos) {
            System.out.println("班级号" + cls);
        }
        System.out.println("---------------------");

        // 打印调试信息
        System.out.println("返回的专业号: " + response.getMajorCodes());
        System.out.println("返回的学号: " + response.getGradeNos());

        return RestResponse.ok(response);
    }

    // 根据专业号获取班级列表（新增）
    @GetMapping("/getClassesByMajor/{majorId}")
    public RestResponse<List<String>> getClassesByMajor(@PathVariable String majorId) {
        // 使用原生 SQL 查询班级
        String sql = "SELECT class_name FROM class WHERE major_no = ?";  // 替换为实际的表名和字段
        List<String> classNos = jdbcTemplate.queryForList(sql, new Object[]{majorId}, String.class);

        // 打印通过专业号获取到的班级号
        System.out.println("-----classNos-----");
        for (String arr : classNos){
            System.out.println(arr);
        }
        System.out.println("-----classNos-----");

        return RestResponse.ok(classNos);
    }

    @GetMapping("/getPaperByClass/{value}")
    public RestResponse<List<String>> getPaperByClass(@PathVariable String value) throws SQLException {
        List<String> paperNames = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 1. 加载驱动程序
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. 连接数据库
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // 3. 查询是否有指定班级的考试记录
            String sql = "SELECT DISTINCT e.paper_name " +
                    "FROM t_exam_paper_answer e " +
                    "JOIN t_user u ON e.create_user = u.id " +
                    "WHERE u.class_name = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value); // 前端传过来的班级号
            rs = pstmt.executeQuery();

            // 4. 处理结果，将存在的试卷名称存入列表中
            while (rs.next()) {
                paperNames.add(rs.getString("paper_name"));
            }

            // 5. 去重操作
            Set<String> uniquePaperNames = new HashSet<>(paperNames);

            if (uniquePaperNames.isEmpty()) {
                // 传递状态码和错误信息
                return RestResponse.fail(404, "No exams found for the class");
            }

            // 6. 将去重后的试卷名称返回给前端
            return RestResponse.ok(new ArrayList<>(uniquePaperNames));

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            // 传递状态码和错误信息
            return RestResponse.fail(500, "Database error occurred");
        } finally {
            // 7. 关闭资源
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

    @PostMapping("/export")
    public ResponseEntity<ExportResponse> exportGrades(@RequestBody ExportQuery query, HttpServletRequest request) throws ClassNotFoundException, SQLException, IOException {
        System.out.println("-----以下将打印/export中的MajorId和class");
        System.out.println(query.getMajorId());
        System.out.println(query.getClassofParam());
        System.out.println(query.getPapersname());

        // 生成饼状图并返回前端
        Map<String, Integer> examData = getExamData(query.getClassofParam(), query.getPapersname());

        for (Map.Entry<String, Integer> entry : examData.entrySet()) {
            System.out.println("Key = " + entry.getKey() + " Value = " + entry.getValue());
        }

        if (examData.isEmpty()) {
            return new ResponseEntity<ExportResponse>(HttpStatus.NOT_FOUND);
        }

        // 分段统计结果
        Map<String, Integer> scoreDistribution = new HashMap<>();
        scoreDistribution.put("不及格", 0);
        scoreDistribution.put("及格", 0);
        scoreDistribution.put("良好", 0);
        scoreDistribution.put("优秀", 0);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 1. 加载驱动程序
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 2. 连接数据库
        conn = DriverManager.getConnection(URL, USER, PASSWORD);

        // 3. 查询目标试卷的总成绩
        String totalScoreSql = "SELECT paper_score FROM t_exam_paper_answer WHERE paper_name = ? LIMIT 1";
        PreparedStatement pstmtTotalScore = conn.prepareStatement(totalScoreSql);
        pstmtTotalScore.setString(1, query.getPapersname()); // 指定试卷名称
        ResultSet rsTotalScore = pstmtTotalScore.executeQuery();

        int totalScore = 0;
        if (rsTotalScore.next()) {
            totalScore = rsTotalScore.getInt("paper_score");
            System.out.println("试卷的总分是: " + totalScore);
        } else {
            System.out.println("未找到指定试卷的总分");
        }

        // 3. 查询该班级中参加指定试卷的学生的最近一次作答成绩
        String sql = "SELECT e.system_score, u.stuNo " +
                "FROM t_exam_paper_answer e " +
                "JOIN t_user u ON e.create_user = u.id " +
                "JOIN (SELECT create_user, MAX(create_time) AS latest_time " +
                "FROM t_exam_paper_answer " +
                "WHERE paper_name = ? " +
                "GROUP BY create_user) latest " +
                "ON e.create_user = latest.create_user AND e.create_time = latest.latest_time " +
                "WHERE u.stuNo LIKE ?";

        // 使用PreparedStatement来执行查询
        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, query.getPapersname()); // 指定试卷名称
        pstmt.setString(2, query.getClassofParam() + "%"); // 根据学号查询该班级的学生
        rs = pstmt.executeQuery();

        // 4. 处理结果，按照分段统计人数
        while (rs.next()) {
            int score = rs.getInt("system_score");

            if (score < totalScore * 0.6) {
                scoreDistribution.put("不及格", scoreDistribution.get("不及格") + 1);
            } else if (score < totalScore * 0.7) {
                scoreDistribution.put("及格", scoreDistribution.get("及格") + 1);
            } else if (score < totalScore * 0.85) {
                scoreDistribution.put("良好", scoreDistribution.get("良好") + 1);
            } else {
                scoreDistribution.put("优秀", scoreDistribution.get("优秀") + 1);
            }
        }

        System.out.println("------------------------");
        for (Map.Entry<String, Integer> a : scoreDistribution.entrySet()) {
            System.out.println("Key = " + a.getKey() + " Value = " + a.getValue());
        }
        System.out.println("------------------------");

        // 生成饼状图
        String pieChartUrl = generatePieChart(scoreDistribution, query.getPapersname());

        // 生成直方图
        String barChartUrl = generateBarChart(scoreDistribution, query.getPapersname());

        // 生成报表
        String headerCellUrl = generateHeadCell(query.getClassofParam(), query.getPapersname());

        String encodedReportUrl = URLEncoder.encode(headerCellUrl, "UTF-8");
// 同样处理其他URL
        String encodedPieChartUrl = URLEncoder.encode(pieChartUrl, "UTF-8");
        String encodedBarChartUrl = URLEncoder.encode(barChartUrl, "UTF-8");

        System.out.println("路径如下：");
        System.out.println(pieChartUrl);
        System.out.println(barChartUrl);
        System.out.println(headerCellUrl);

        // 创建响应对象
        ExportResponse exportResponse = new ExportResponse(encodedPieChartUrl, encodedBarChartUrl, encodedReportUrl);

        // 返回响应体
        return new ResponseEntity<>(exportResponse, HttpStatus.OK);

        // 返回结果 return null;
        //return ResponseEntity.ok().body(Map.of("pieChartUrl", pieChartUrl, "barChartUrl", barChartUrl));
    }

    private String generateHeadCell(String classofParam, String paperName) throws ClassNotFoundException, SQLException, IOException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 1. 加载驱动程序
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 2. 连接数据库
        conn = DriverManager.getConnection(URL, USER, PASSWORD);

        // 查询学生及其考试信息的SQL
        String sql = "SELECT e.exam_paper_id, e.paper_name, e.paper_score, e.system_score, e.question_count, e.question_correct, e.create_time, " +
                "u.school, u.class_name, u.real_name, u.user_name, u.stuNo " +
                "FROM t_exam_paper_answer e " +
                "JOIN t_user u ON e.create_user = u.id " +
                "JOIN (SELECT create_user, MAX(create_time) AS latest_time " +
                "FROM t_exam_paper_answer " +
                "WHERE paper_name = ? " +
                "GROUP BY create_user) latest " +
                "ON e.create_user = latest.create_user AND e.create_time = latest.latest_time " +
                "WHERE u.stuNo LIKE ?";

        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, paperName); // 指定试卷名称
        pstmt.setString(2, classofParam + "%"); // 根据学号查询该班级的学生
        rs = pstmt.executeQuery();

        // 生成Excel报表
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("成绩报表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "试卷ID", "试卷名称", "试卷总分", "用户得分", "题目总数", "用户答对的题数", "答卷时间",
                "学校", "班级", "用户姓名", "用户名", "用户学号"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderCellStyle(workbook)); // 设置样式
        }

        // 填充数据
        int rowNum = 1;
        while (rs.next()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rs.getInt("exam_paper_id"));
            row.createCell(1).setCellValue(rs.getString("paper_name"));
            row.createCell(2).setCellValue(rs.getInt("paper_score"));
            row.createCell(3).setCellValue(rs.getInt("system_score"));
            row.createCell(4).setCellValue(rs.getInt("question_count"));
            row.createCell(5).setCellValue(rs.getInt("question_correct"));
            row.createCell(6).setCellValue(rs.getTimestamp("create_time").toString());
            row.createCell(7).setCellValue(rs.getString("school"));
            row.createCell(8).setCellValue(rs.getString("class_name"));
            row.createCell(9).setCellValue(rs.getString("real_name"));
            row.createCell(10).setCellValue(rs.getString("user_name"));
            row.createCell(11).setCellValue(rs.getString("stuNo"));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 将Excel保存到文件
        String filePath = "成绩报表.xlsx";
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
            System.out.println("Excel报表已保存到: " + new File(filePath).getAbsolutePath());
            filePath = new File(filePath).getAbsolutePath();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            workbook.close(); // 关闭工作簿
        }

        // 返回Excel文件路径或其他信息给前端
        return filePath;
        //return ResponseEntity.ok(filePath); // 返回文件路径给前端

    }

    // 创建表头样式
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 使用 Apache POI 的 Font 创建字体样式
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true); // 设置字体加粗
        style.setFont(font); // 将字体样式应用到单元格样式中

        return style;
    }

    // 生成直方图的方法
    // 生成直方图的方法
    private String generateBarChart(Map<String, Integer> scoreDistribution, String paperName) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entry : scoreDistribution.entrySet()) {
            dataset.addValue(entry.getValue(), "学生人数", entry.getKey());
        }

        // 创建直方图
        JFreeChart barChart = ChartFactory.createBarChart(
                paperName + " 成绩分布直方图", // 图表标题
                "分数段", // X 轴标签
                "学生人数", // Y 轴标签
                dataset // 数据集
        );

        // 自定义直方图样式
        CategoryPlot plot = barChart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(79, 129, 189)); // 设置柱状图颜色
        renderer.setItemMargin(0.02); // 柱子间的间隔

        // 设置字体防止中文乱码
        Font font = new Font("微软雅黑", Font.PLAIN, 12); // 设置字体为微软雅黑
        barChart.getTitle().setFont(new Font("微软雅黑", Font.BOLD, 20)); // 设置标题字体
        plot.getDomainAxis().setLabelFont(font); // X 轴标签字体
        plot.getRangeAxis().setLabelFont(font); // Y 轴标签字体
        plot.getDomainAxis().setTickLabelFont(font); // X 轴刻度字体
        plot.getRangeAxis().setTickLabelFont(font); // Y 轴刻度字体

        // 将直方图保存为图片
        String filePath = "成绩分布直方图.jpeg";
        try {
            File barChartFile = new File(filePath);
            ChartUtils.saveChartAsJPEG(barChartFile, barChart, 800, 600);
            System.out.println("直方图已保存到: " + barChartFile.getAbsolutePath());
            filePath = barChartFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filePath; // 返回图片的路径
    }

    private Map<String, Integer> getExamData(String className, String paperName) {
        Map<String, Integer> examData = new HashMap<>();

        try {
            // 1. 加载驱动程序
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. 获得数据库的连接
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/xzs", "root", "123456");

            // 3. 查询 t_user 表获取该班级学生的 id
            String userQuery = "SELECT id FROM t_user WHERE class_name = ?";
            PreparedStatement pstmtUser = conn.prepareStatement(userQuery);
            pstmtUser.setString(1, className);
            ResultSet rsUser = pstmtUser.executeQuery();

            // 4. 查询 t_exam_paper_answer 表获取指定试卷的考试信息
            String examQuery = "SELECT system_score, paper_score FROM t_exam_paper_answer WHERE create_user = ? AND paper_name = ?";
            PreparedStatement pstmtExam = conn.prepareStatement(examQuery);



            while (rsUser.next()) {
                int userId = rsUser.getInt("id");
                pstmtExam.setInt(1, userId);
                pstmtExam.setString(2, paperName);
                ResultSet rsExam = pstmtExam.executeQuery();

                while (rsExam.next()) {
                    int userScore = rsExam.getInt("system_score");
                    int paperScore = rsExam.getInt("paper_score");
                    examData.put("用户 " + userId, userScore);
                }
            }

            pstmtUser.close();
            pstmtExam.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return examData;
    }

    // 生成饼状图
    private String generatePieChart(Map<String, Integer> scoreDistribution, String paperName) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        int totalStudents = 0;

        for (Integer count : scoreDistribution.values()) {
            totalStudents += count;
        }

        for (Map.Entry<String, Integer> entry : scoreDistribution.entrySet()) {
            String label = entry.getKey() + " (" + String.format("%.2f", (entry.getValue() * 100.0 / totalStudents)) + "%)";
            dataset.setValue(label, entry.getValue());
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "考试成绩分布 - " + paperName,
                dataset,
                true,
                true,
                false
        );

        Font titleFont = new Font("SimSun", Font.PLAIN, 18);
        if (titleFont != null) {
            pieChart.getTitle().setFont(titleFont);
        }

        Font legendFont = new Font("SimSun", Font.PLAIN, 14);
        if (legendFont != null) {
            pieChart.getLegend().setItemFont(legendFont);
        }

        PiePlot plot = (PiePlot) pieChart.getPlot();
        Font labelFont = new Font("SimSun", Font.PLAIN, 12);
        if (labelFont != null) {
            plot.setLabelFont(labelFont);
        }

        String chartPath = "C:\\Users\\HW\\Desktop\\examination\\examScoresPieChart.png";
        String filePath = "成绩饼状图.jpeg";
        File ChartFile = new File(filePath);

        filePath = new File(filePath).getAbsolutePath();
        File pieChartFile = new File(filePath);

        try {
            ChartUtils.saveChartAsPNG(pieChartFile, pieChart, 800, 600);
            System.out.println("饼状图成功保存至" + pieChartFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    // 封装可视化结果
    public class ExportResponse {
        private String pieChartUrl;
        private String barChartUrl;
        private String reportUrl;

        // Constructors, getters, and setters
        public ExportResponse(String pieChartUrl, String barChartUrl, String reportUrl) {
            this.pieChartUrl = pieChartUrl;
            this.barChartUrl = barChartUrl;
            this.reportUrl = reportUrl;
        }

        public String getPieChartUrl() {
            return pieChartUrl;
        }

        public void setPieChartUrl(String pieChartUrl) {
            this.pieChartUrl = pieChartUrl;
        }

        public String getBarChartUrl() {
            return barChartUrl;
        }

        public void setBarChartUrl(String barChartUrl) {
            this.barChartUrl = barChartUrl;
        }

        public String getReportUrl() {
            return reportUrl;
        }

        public void setReportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
        }
    }


    // 内部类用于封装年级号、专业号、班级号和学号列表的响应
    public static class MajorResponse {
        private List<String> gradeNos; // 年级号列表
        private List<String> majorCodes; // 专业号列表
        private List<String> classNos; // 班级号列表
        private List<String> stuNos; // 学号列表

        public List<String> getPaperName() {
            return paperName;
        }

        public void setPaperName(List<String> paperName) {
            this.paperName = paperName;
        }

        private List<String> paperName; // 试卷名称

        // Getters and Setters
        public List<String> getGradeNos() {
            return gradeNos;
        }

        public void setGradeNos(List<String> gradeNos) {
            this.gradeNos = gradeNos;
        }

        public List<String> getMajorCodes() {
            return majorCodes;
        }

        public void setMajorCodes(List<String> majorCodes) {
            this.majorCodes = majorCodes;
        }

        public List<String> getClassNos() {
            return classNos;
        }

        public void setClassNos(List<String> classNos) {
            this.classNos = classNos;
        }

        public List<String> getStuNos() {
            return stuNos;
        }

        public void setStuNos(List<String> stuNos) {
            this.stuNos = stuNos;
        }
    }

    private static class ExportQuery {
        public Long getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }

        public String getMajorId() {
            return majorId;
        }

        public void setMajorId(String majorId) {
            this.majorId = majorId;
        }

        public Integer getPageIndex() {
            return pageIndex;
        }

        public void setPageIndex(Integer pageIndex) {
            this.pageIndex = pageIndex;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        private Long subjectId; // 或者相应的类型
        private String majorId;
        private Integer pageIndex;
        private Integer pageSize;

        public String getPapersname() {
            return papersname;
        }

        public void setPapersname(String papersname) {
            this.papersname = papersname;
        }

        private String papersname;

        public String getClassofParam() {
            return classofParam;
        }

        public void setClassofParam(String classofParam) {
            this.classofParam = classofParam;
        }

        private String classofParam; // 注意这里是 class，可能需要重命名
        // 生成 getter 和 setter 方法
    }
}
