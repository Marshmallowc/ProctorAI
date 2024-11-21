package com.mindskip.xzs.controller.student;

import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.*;
import com.mindskip.xzs.domain.enums.ExamPaperAnswerStatusEnum;
import com.mindskip.xzs.event.CalculateExamPaperAnswerCompleteEvent;
import com.mindskip.xzs.event.UserEvent;
import com.mindskip.xzs.service.ExamPaperAnswerService;
import com.mindskip.xzs.service.ExamPaperService;
import com.mindskip.xzs.service.SubjectService;
import com.mindskip.xzs.utility.DateTimeUtil;
import com.mindskip.xzs.utility.ExamUtil;
import com.mindskip.xzs.utility.PageInfoHelper;
import com.mindskip.xzs.viewmodel.admin.exam.ExamPaperEditRequestVM;
import com.mindskip.xzs.viewmodel.student.exam.ExamPaperReadVM;
import com.mindskip.xzs.viewmodel.student.exam.ExamPaperSubmitItemVM;
import com.mindskip.xzs.viewmodel.student.exam.ExamPaperSubmitVM;
import com.mindskip.xzs.viewmodel.student.exampaper.ExamPaperAnswerPageResponseVM;
import com.mindskip.xzs.viewmodel.student.exampaper.ExamPaperAnswerPageVM;
import com.github.pagehelper.PageInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController("StudentExamPaperAnswerController")
@RequestMapping(value = "/api/student/exampaper/answer")
public class ExamPaperAnswerController extends BaseApiController {

    private final ExamPaperAnswerService examPaperAnswerService;
    private final ExamPaperService examPaperService;
    private final SubjectService subjectService;
    private final ApplicationEventPublisher eventPublisher;
    private static final String URL = "jdbc:mysql://127.0.0.1:3307/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    @Autowired
    public ExamPaperAnswerController(ExamPaperAnswerService examPaperAnswerService, ExamPaperService examPaperService, SubjectService subjectService, ApplicationEventPublisher eventPublisher) {
        this.examPaperAnswerService = examPaperAnswerService;
        this.examPaperService = examPaperService;
        this.subjectService = subjectService;
        this.eventPublisher = eventPublisher;
    }


    @RequestMapping(value = "/pageList", method = RequestMethod.POST)
    public RestResponse<PageInfo<ExamPaperAnswerPageResponseVM>> pageList(@RequestBody @Valid ExamPaperAnswerPageVM model) {
        model.setCreateUser(getCurrentUser().getId());
        PageInfo<ExamPaperAnswer> pageInfo = examPaperAnswerService.studentPage(model);
        PageInfo<ExamPaperAnswerPageResponseVM> page = PageInfoHelper.copyMap(pageInfo, e -> {
            ExamPaperAnswerPageResponseVM vm = modelMapper.map(e, ExamPaperAnswerPageResponseVM.class);
            Subject subject = subjectService.selectById(vm.getSubjectId());
            vm.setDoTime(ExamUtil.secondToVM(e.getDoTime()));
            vm.setSystemScore(ExamUtil.scoreToVM(e.getSystemScore()));
            vm.setUserScore(ExamUtil.scoreToVM(e.getUserScore()));
            vm.setPaperScore(ExamUtil.scoreToVM(e.getPaperScore()));
            vm.setSubjectName(subject.getName());
            vm.setCreateTime(DateTimeUtil.dateFormat(e.getCreateTime()));
            return vm;
        });
        return RestResponse.ok(page);
    }

    public static String getCorrectValue(int questionId) {
        String sql = "SELECT content FROM t_text_content WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置查询参数
            stmt.setInt(1, questionId);

            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString("content");

                    // 提取 JSON 字段中的 correct 值
                    if (content != null) {
                        return extractCorrectFromJson(content);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // 提取 JSON 中的 correct 字段
    private static String extractCorrectFromJson(String json) {
        try {
            // 使用 org.json 或其他 JSON 解析库
            org.json.JSONObject jsonObject = new org.json.JSONObject(json);
            return jsonObject.getString("correct");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getInfoTextContentId(String questionId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String infoTextContentId = null;

        try {
            // 建立数据库连接
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // 查询 info_text_content_id
            String query = "SELECT info_text_content_id FROM t_question WHERE id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, questionId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                infoTextContentId = resultSet.getString("info_text_content_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return infoTextContentId;
    }

    /**
     * 从 t_text_content 表中提取 content 属性并解析 JSON 数据
     */
    public static List<String> extractQuestionItemContents(String questionId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> ret = new ArrayList<>();

        try {
            // 建立数据库连接
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // 查询 `content` 字段
            String query = "SELECT content FROM t_text_content WHERE id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, questionId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String content = resultSet.getString("content");

                // 使用 org.json 解析 JSON 数据
                JSONObject jsonObject = new JSONObject(content);
                JSONArray questionItemObjects = jsonObject.getJSONArray("questionItemObjects");

                // 遍历 `questionItemObjects` 并提取 `prefix` 和 `content`
                for (int i = 0; i < questionItemObjects.length(); i++) {
                    JSONObject item = questionItemObjects.getJSONObject(i);

                    // 根据类型获取 `prefix`
                    String prefix = item.optString("prefix", String.valueOf(item.optInt("prefix")));
                    String contentValue = item.getString("content");

                    // 输出 `prefix` 和对应的 `content`
                    System.out.println("Prefix: " + prefix + ", Content: " + contentValue);
                    ret.add(contentValue);
                }
            } else {
                System.out.println("No data found for question_id: " + questionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    @RequestMapping(value = "/answerSubmit", method = RequestMethod.POST)
    public RestResponse answerSubmit(@RequestBody @Valid ExamPaperSubmitVM examPaperSubmitVM) {
        User user = getCurrentUser();
        ExamPaperAnswerInfo examPaperAnswerInfo = examPaperAnswerService.calculateExamPaperAnswer(examPaperSubmitVM, user);
        if (null == examPaperAnswerInfo) {
            return RestResponse.fail(2, "试卷不能重复做");
        }
        ExamPaperAnswer examPaperAnswer = examPaperAnswerInfo.getExamPaperAnswer();
        Integer userScore = examPaperAnswer.getUserScore();

        String scoreVm = ExamUtil.scoreToVM(userScore);
        UserEventLog userEventLog = new UserEventLog(user.getId(), user.getUserName(), user.getRealName(), new Date());
        String content = user.getUserName() + " 提交试卷：" + examPaperAnswerInfo.getExamPaper().getName()
                + " 得分：" + scoreVm
                + " 耗时：" + ExamUtil.secondToVM(examPaperAnswer.getDoTime());
        userEventLog.setContent(content);
        eventPublisher.publishEvent(new CalculateExamPaperAnswerCompleteEvent(examPaperAnswerInfo));
        eventPublisher.publishEvent(new UserEvent(userEventLog));
        Integer type = 4;
        // 开始进行主观题评判
        // 遍历提交的答案项
        for (ExamPaperSubmitItemVM vm : examPaperSubmitVM.getAnswerItems()) {
            Integer questionId = vm.getQuestionId();
            System.out.println("当前题目ID: " + questionId);

            // 查询题目信息
            String queryQuestion = "SELECT question_type, correct, score FROM t_question WHERE id = ?";
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(queryQuestion)) {
                ps.setLong(1, questionId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int questionType = rs.getInt("question_type");
                    String correctAnswer = rs.getString("correct");
                    int totalScore = rs.getInt("score");

                    System.out.println("题目类型: " + questionType + ", 正确答案: " + correctAnswer + ", 总分: " + totalScore + "题目的编号是：" + questionId);
                    System.out.println("获取到的correct是空的吗 " + correctAnswer.isEmpty());
                    System.out.println("correctAnswer的大小是 " + correctAnswer.length());
                    if (correctAnswer.isEmpty()) {
                        System.out.println("correctAnswer为空");
                        correctAnswer = getCorrectValue(questionId);
                        System.out.println("修改完后的correctAnswer: " + correctAnswer);

                    }

                    // 如果还是没有correctAnswer，就去t_text_content中的content找
                    List<String> judgeAnswer = new ArrayList<>();
                    if (correctAnswer == null || correctAnswer.isEmpty()) {
                        type = 5; // 题目为判断题
                        System.out.println("进入extractQuestionItemContents部分");
                        String infoTextContentId = getInfoTextContentId(questionId.toString()); // 根据 t_question 获取 info_text_content_id
                        if (infoTextContentId != null) {
                            judgeAnswer = extractQuestionItemContents(infoTextContentId); // 提取 t_text_content 数据
                        } else {
                            System.out.println("No info_text_content_id found for question_id: " + questionId);
                        }
                        System.out.println("修改完后的correctAnswer: " + correctAnswer);

                    }

                    for (String arr : vm.getContentArray()) {
                        System.out.println("arr" + arr);
                    }
                    // 判断是否需要修改
                    if (questionType == 4 || questionType == 5) {
                        String studentAnswer = questionType == 4 ? String.join(",", vm.getContentArray()) : vm.getContent();
                        if(studentAnswer.isEmpty()){
                            studentAnswer = vm.getContent();
                            System.out.println("修改了学生答案为" + studentAnswer);
                        }
                        System.out.println("学生答案: " + studentAnswer);

                        // 调用评分函数
                        List<String> final_correctAnswer = new ArrayList<>();
                        if(correctAnswer == "" || correctAnswer.isEmpty()){
                            final_correctAnswer = judgeAnswer;
                        }
                        else{
                            final_correctAnswer.add(correctAnswer);
                        }

                        // 将学生答案变成列表
                        List<String> final_studentAnswer = new ArrayList<>();
                        if(type == 5){
                            final_studentAnswer = convertStringToList(studentAnswer);
                        }
                        else {
                            final_studentAnswer.add(studentAnswer);
                        }
                        double accuracy = AI_Grade(final_studentAnswer, final_correctAnswer);
                        int studentScore = (int) (accuracy * totalScore);
                        System.out.println("学生得分: " + studentScore);
                        // 更新学生的总分
                        String updateScoreQuery = "UPDATE t_exam_paper_answer " + "SET system_score = system_score + ? " + "WHERE exam_paper_id = ? AND create_user = ? " + "ORDER BY id DESC LIMIT 1";
                        try (PreparedStatement updatePs = connection.prepareStatement(updateScoreQuery)) {
                            updatePs.setInt(1, studentScore);
                            updatePs.setLong(2, examPaperAnswerInfo.getExamPaper().getId());
                            updatePs.setLong(3, user.getId());
                            int rowsUpdated = updatePs.executeUpdate();
                            System.out.println("更新成功的行数: " + rowsUpdated);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return RestResponse.ok(scoreVm);
    }

    // 模拟评分函数（需根据实际逻辑实现）
    private double AI_Grade(List<String> studentAnswer, List<String> correctAnswer) {

        for(String i : studentAnswer){
            System.out.println("studentAnswer: " + i);
        }
        for(String i : correctAnswer){
            System.out.println("correctAnswer: " + i);
        }
        System.out.println("进入AI评分模块");

        return 1.0;
    }

    public static List<String> convertStringToList(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(); // 返回空列表
        }

        // 根据逗号分隔符分割并去掉多余空格
        List<String> list = Arrays.asList(input.split(",\\s*"));
        return list;
    }


    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public RestResponse edit(@RequestBody @Valid ExamPaperSubmitVM examPaperSubmitVM) {
        boolean notJudge = examPaperSubmitVM.getAnswerItems().stream().anyMatch(i -> i.getDoRight() == null && i.getScore() == null);
        if (notJudge) {
            return RestResponse.fail(2, "有未批改题目");
        }

        ExamPaperAnswer examPaperAnswer = examPaperAnswerService.selectById(examPaperSubmitVM.getId());
        ExamPaperAnswerStatusEnum examPaperAnswerStatusEnum = ExamPaperAnswerStatusEnum.fromCode(examPaperAnswer.getStatus());
        if (examPaperAnswerStatusEnum == ExamPaperAnswerStatusEnum.Complete) {
            return RestResponse.fail(3, "试卷已完成");
        }
        String score = examPaperAnswerService.judge(examPaperSubmitVM);
        User user = getCurrentUser();
        UserEventLog userEventLog = new UserEventLog(user.getId(), user.getUserName(), user.getRealName(), new Date());
        String content = user.getUserName() + " 批改试卷：" + examPaperAnswer.getPaperName() + " 得分：" + score;
        userEventLog.setContent(content);
        eventPublisher.publishEvent(new UserEvent(userEventLog));




        return RestResponse.ok(score);
    }

    @RequestMapping(value = "/read/{id}", method = RequestMethod.POST)
    public RestResponse<ExamPaperReadVM> read(@PathVariable Integer id) {
        ExamPaperAnswer examPaperAnswer = examPaperAnswerService.selectById(id);
        ExamPaperReadVM vm = new ExamPaperReadVM();
        ExamPaperEditRequestVM paper = examPaperService.examPaperToVM(examPaperAnswer.getExamPaperId());
        ExamPaperSubmitVM answer = examPaperAnswerService.examPaperAnswerToVM(examPaperAnswer.getId());
        vm.setPaper(paper);
        vm.setAnswer(answer);
        return RestResponse.ok(vm);
    }


}
