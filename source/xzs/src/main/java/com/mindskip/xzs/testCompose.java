package com.mindskip.xzs;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class testCompose {
    // 题目数据结构
    public static class Question {
        public int id;
        String questionType;
        String subjectId;
        public int score;
        String gradeLevel;
        String difficulty;
        public String content;
        public String knowledgePoint;
        public Question(){
        }
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
    // JDBC数据库连接参数
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    // 从数据库获取题目的方法
    public static List<Question> getQuestionsFromDatabase(String gradeLevel, String subject, int difficulty) {
        List<Question> questions = new ArrayList<>();
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
            statement.setInt(3, difficulty);
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
                Question question = new Question(id, questionType, subjectId, score, grade, difficultyLevel, content, knowledgePoint);
                questions.add(question);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }
    // 贪心算法根据知识点和分数选题
    public static List<Question> generatePaper(List<Question> questions, Map<String, Integer> knowledgePoints) {
        List<Question> selectedQuestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : knowledgePoints.entrySet()) {
            String knowledgePoint = entry.getKey();
            int targetScore = entry.getValue();
            System.out.println("这里是"+knowledgePoint);
            // 找出当前知识点相关的题目
            List<Question> filteredQuestions = new ArrayList<>();
            for (Question q : questions) {
                if (q.knowledgePoint.contains(knowledgePoint)) {
                    filteredQuestions.add(q);
                }
            }
            System.out.println("filteredQuestions的数量为" + filteredQuestions.size());
            // 按分数从高到低排序
            Collections.sort(filteredQuestions, Comparator.comparingInt((Question q) -> q.score).reversed());
            // 贪心算法选择分数接近要求的题目
            int currentScore = 0;
            for (Question q : filteredQuestions) {
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
    public static void main(String[] args) {
        // 从数据库获取符合条件的题目
        List<Question> questions = getQuestionsFromDatabase("1", "6", 3);
        System.out.println("题目数量"+questions.size());
        // 设置前端传入的知识点和分数
        Map<String, Integer> knowledgePoints = new HashMap<>();
        knowledgePoints.put("二叉树", 50);
        knowledgePoints.put("广义表", 70);
        knowledgePoints.put("矩阵", 30);
        knowledgePoints.put("huffman编码", 110);
        knowledgePoints.put("迪杰斯特拉算法", 20);
        // 生成试卷
        List<Question> selectedQuestions = generatePaper(questions, knowledgePoints);
        // 打印选中的题目编号和分数
        System.out.println("生成的试卷题目：");
        System.out.println(selectedQuestions.size());
        for (Question q : selectedQuestions) {
            System.out.println("题目ID: " + q.id + "，分数: " + q.score + "，内容: " + q.content);
        }
    }
}
