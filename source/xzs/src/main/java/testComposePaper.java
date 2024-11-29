import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.controller.admin.ExamPaperController;
import com.mindskip.xzs.domain.ExamPaper;
import com.mindskip.xzs.testCompose;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javax.validation.Valid;
import java.sql.*;
import java.util.*;
import java.util.Date;
public class testComposePaper {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    private String grade;
    private String subject;
    private String difficulty;
    public static void main(String[] args) {
        new testComposePaper().compose();
    }
    // 一键组卷功能
    public void compose() {
        // ExamPaperController.PaperData.KnowledgePoint test_knowledges = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a0 = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a1 = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a2 = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a3 = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a4 = new ExamPaperController.PaperData.KnowledgePoint();
        ExamPaperController.PaperData.KnowledgePoint a5 = new ExamPaperController.PaperData.KnowledgePoint();
        a0.setLabel("矩阵");
        a0.setScore(6);
        List<ExamPaperController.PaperData.KnowledgePoint> list_knowledge = new ArrayList<>();
        list_knowledge.add(a0);
        a1.setLabel("哈夫曼树");
        a1.setScore(11);
        list_knowledge.add(a1);
        a2.setLabel("图论");
        a2.setScore(11);
        list_knowledge.add(a2);
        a3.setLabel("时间复杂度");
        a3.setScore(3);
        list_knowledge.add(a3);
        a4.setLabel("数组");
        a4.setScore(7);
        list_knowledge.add(a4);
        a5.setLabel("树");
        a5.setScore(15);
        list_knowledge.add(a5);
        System.out.println("list_knowledges的长度是" + list_knowledge.size());
        for(ExamPaperController.PaperData.KnowledgePoint i : list_knowledge){
            System.out.println(i.getLabel());
        }
        ExamPaperController.PaperData model = new ExamPaperController.PaperData();
        model.setDifficulty("中");
        model.setGrade("大一");
        model.setPaperName("测试卷");
        model.setSubject("数据结构");
        model.setSuggestedDuration(90);
        model.setKnowledgePoints(list_knowledge);
        // 格式化知识点-分数
        Map<String, Integer> knowledgePoints = new HashMap<>();
        model.getKnowledgePoints().forEach(knowledgePoint -> {
            knowledgePoints.put(knowledgePoint.getLabel(), knowledgePoint.getScore()*10);
            System.out.println("检查knowledge的MAP " + knowledgePoint.getLabel() + " : " + knowledgePoint.getScore());
        });
        // 规范前端传来的数据
        OrgnizeData(model.getGrade(), model.getSubject(), model.getDifficulty());
        // 从数据库获取符合条件的题目
        List<testCompose.Question> questions = getQuestionsFromDatabase(this.grade, this.subject, this.difficulty);
        System.out.println("题目数量"+questions.size());
        // 生成试卷
        List<testCompose.Question> selectedQuestions = generatePaper(questions, knowledgePoints);
        // 打印选中的题目编号和分数
        System.out.println("生成的试卷题目：");
        System.out.println(selectedQuestions.size());
        int paperScore = 0;
        for (testCompose.Question q : selectedQuestions) {
            System.out.println("题目ID: " + q.id + "，分数: " + q.score + "，内容: " + q.content + "编号" + q.id);
            paperScore += q.score;
        }
        // 修改为组卷的模式
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");
        int itemOrder = 1;
        for (testCompose.Question q : selectedQuestions) {
            jsonBuilder.append("{\"name\":\"题目").append(itemOrder).append("\",");
            jsonBuilder.append("\"questionItems\":[{\"id\":").append(q.id).append(",\"itemOrder\":").append(itemOrder).append("}]},");
            itemOrder++;
        }
        // 删除最后一个逗号，并关闭 JSON 数组
        if (jsonBuilder.length() > 1) {
            jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
        }
        jsonBuilder.append("]");
        // 打印生成的 JSON 字符串
        String jsonOutput = jsonBuilder.toString();
        System.out.println("生成的试卷JSON内容: " + jsonOutput);
//        // 将试卷json插入t_text_content，之后获取id，幅值给frame_text_content_id
//        long insertID = insertPaperContent(jsonOutput);
//        System.out.println("成功插入t_text_content");
//
//        // 先将试卷信息插入t_exam_paper(name, subject_id, paper_type, grade_level, score, question_count, suggest_time, limit_start_time, limit_end_time, frame_text_content_id, create_user, create_time)
//        insertExamPaper(insertID, model.getPaperName(), this.subject, 1, this.grade, paperScore / 10, selectedQuestions.size(), model.getSuggestedDuration(), 2);
//        System.out.println("成功插入t_exam_paper");
        // 将这条记录同步到t_user_event_log(id, user_id, user_name, real_name, content)
        System.out.println("组卷成功嘻嘻嘻嘻嘻");
        //return RestResponse.ok("组卷成功！");
    }
    public void insertExamPaper(long insertID, String name, String subject_id, int paper_type, String grade_level,
                                int score, int question_count, int suggest_time, int create_user) {
        String insertExamPaperSql = "INSERT INTO t_exam_paper (name, subject_id, paper_type, grade_level, " +
                "score, question_count, suggest_time, limit_start_time, limit_end_time, " +
                "frame_text_content_id, create_user, create_time, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // 当前时间
        Date create_time = new Date();
        Timestamp currentTime = new Timestamp(create_time.getTime());
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(insertExamPaperSql)) {
            // 设置参数
            statement.setString(1, name);
            statement.setString(2, subject_id);
            statement.setInt(3, paper_type);
            statement.setString(4, grade_level);
            statement.setInt(5, score);
            statement.setInt(6, question_count);
            statement.setInt(7, suggest_time);
            statement.setTimestamp(8, null);  // 假设 limit_start_time 为当前时间
            statement.setTimestamp(9, null);  // 假设 limit_end_time 为当前时间
            statement.setLong(10, insertID);          // frame_text_content_id
            statement.setInt(11, create_user);
            statement.setTimestamp(12, currentTime);  // create_time
            statement.setBoolean(13, false);  // deleted
            // 执行插入
            statement.executeUpdate();
            System.out.println("试卷信息插入成功！");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public long insertPaperContent(String paperContentJson){
        String insertSql = "INSERT INTO t_text_content (content, create_time) VALUES (?, ?)";
        String generatedColumns[] = { "id" }; // 用于获取自增ID
        long insertID = 0;
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(insertSql, generatedColumns)) {
            // 设置content字段的值
            statement.setString(1, paperContentJson);
            // 设置create_time字段的值为当前时间
            statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            // 执行插入
            int affectedRows = statement.executeUpdate();
            // 获取生成的自增ID
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        insertID = generatedKeys.getLong(1); // 获取生成的ID
                        System.out.println("插入成功，生成的ID为: " + insertID);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return insertID;
    }
    public static List<testCompose.Question> getQuestionsFromDatabase(String gradeLevel, String subject, String difficulty) {
        System.out.println("getQuestionsFromDatabase中传来：grade = " + gradeLevel + " subject = " + subject + " difficulty = " + difficulty);
        List<testCompose.Question> questions = new ArrayList<>();
        // SQL 查询语句，获取 t_question 和 t_text_content 表的数据
        String sql = "SELECT q.id, q.question_type, q.subject_id, q.score, q.grade_level, q.difficult, t.content, q.knowledge_points " +
                "FROM t_question q " +
                "JOIN t_text_content t ON q.info_text_content_id = t.id " +
                "WHERE q.grade_level = ? AND q.subject_id = ? AND q.difficult = ?";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            // 设置查询条件参数
            statement.setString(1, gradeLevel);
            statement.setString(2, subject);
            statement.setString(3, difficulty);
            // 执行查询
            ResultSet resultSet = statement.executeQuery();
            // 遍历结果集并转换为 Question 对象
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String questionType = resultSet.getString("question_type");
                String subjectId = resultSet.getString("subject_id");
                int score = resultSet.getInt("score");
                String grade = resultSet.getString("grade_level");
                String difficultyLevel = resultSet.getString("difficult");
                String content = resultSet.getString("content");
                String knowledgePoint = resultSet.getString("knowledge_points");
                // 将结果封装成 Question 对象
                testCompose.Question question = new testCompose.Question(id, questionType, subjectId, score, grade, difficultyLevel, content, knowledgePoint);
                questions.add(question);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }
    // 贪心算法根据知识点和分数选题
    public static List<testCompose.Question> generatePaper(List<testCompose.Question> questions, Map<String, Integer> knowledgePoints) {
        List<testCompose.Question> selectedQuestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : knowledgePoints.entrySet()) {
            String knowledgePoint = entry.getKey();
            int targetScore = entry.getValue();
            System.out.println("这里是"+knowledgePoint);
            // 找出当前知识点相关的题目
            List<testCompose.Question> filteredQuestions = new ArrayList<>();
            for (testCompose.Question q : questions) {
                if (q.knowledgePoint == null)
                    continue;
                if (q.knowledgePoint.contains(knowledgePoint)) {
                    filteredQuestions.add(q);
                }
            }
            System.out.println("filteredQuestions的数量为" + filteredQuestions.size());
            // 按分数从高到低排序
            Collections.sort(filteredQuestions, Comparator.comparingInt((testCompose.Question q) -> q.score).reversed());
            // 贪心算法选择分数接近要求的题目
            int currentScore = 0;
            for (testCompose.Question q : filteredQuestions) {
                if (currentScore + q.score <= targetScore) {
                    selectedQuestions.add(q);
                    currentScore += q.score;
                }
                if (currentScore == targetScore) {
                    break;
                }
            }
        }
        // 乱序题目
        Collections.shuffle(selectedQuestions);
        return selectedQuestions;
    }
    public void OrgnizeData(String grade, String subject, String difficulty){
        // 格式化年级
        if(grade.equals("大一"))  grade = "1";
        else if(grade.equals("大二")) grade = "2";
        else if(grade.equals("大三")) grade = "3";
        else if(grade.equals("大四")) grade = "4";
        else if(grade.equals("研一")) grade = "5";
        else if(grade.equals("研二")) grade = "6";
        else grade = "7";
        // 格式化学科
        if(subject.equals("数据结构"))   subject = "1";
        // 格式化困难度
        if(difficulty.equals("难")) difficulty = "3";
        else if(difficulty.equals("中")) difficulty = "2";
        else difficulty = "1";
        this.grade = grade;
        this.subject = subject;
        this.difficulty = difficulty;
    }
    public static class Question {
        int id;
        String questionType;
        String subjectId;
        int score;
        String gradeLevel;
        String difficulty;
        String content;
        String knowledgePoint;
        public Question(int id, String questionType, String subjectId, int score, String gradeLevel, String difficulty, String content, String knowledgePoint) {
            this.id = id;
            this.questionType = questionType;
            this.subjectId = subjectId;
            this.score = score;
            this.gradeLevel = gradeLevel;
            this.difficulty = difficulty;
            this.content = content;
            this.knowledgePoint = knowledgePoint;
        }
    }
}
