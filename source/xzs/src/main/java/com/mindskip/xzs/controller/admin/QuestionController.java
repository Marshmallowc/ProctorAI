package com.mindskip.xzs.controller.admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.base.SystemCode;
import com.mindskip.xzs.domain.Question;
import com.mindskip.xzs.domain.TextContent;
import com.mindskip.xzs.domain.enums.QuestionTypeEnum;
import com.mindskip.xzs.domain.question.QuestionObject;
import com.mindskip.xzs.service.QuestionService;
import com.mindskip.xzs.service.TextContentService;
import com.mindskip.xzs.utility.*;
import com.mindskip.xzs.viewmodel.admin.question.QuestionEditRequestVM;
import com.mindskip.xzs.viewmodel.admin.question.QuestionPageRequestVM;
import com.mindskip.xzs.viewmodel.admin.question.QuestionResponseVM;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.Function;

@RestController("AdminQuestionController")
@RequestMapping(value = "/api/admin/question")
public class QuestionController extends BaseApiController {
    private final QuestionService questionService;
    private final TextContentService textContentService;
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    @Autowired
    public QuestionController(QuestionService questionService, TextContentService textContentService) {
        this.questionService = questionService;
        this.textContentService = textContentService;
    }

    public QuestionController(){
        this.questionService = new QuestionService() {
            @Override
            public PageInfo<Question> page(QuestionPageRequestVM requestVM) {
                return null;
            }

            @Override
            public Question insertFullQuestion(QuestionEditRequestVM model, Integer userId) {
                return null;
            }

            @Override
            public Question updateFullQuestion(QuestionEditRequestVM model) {
                return null;
            }

            @Override
            public QuestionEditRequestVM getQuestionEditRequestVM(Integer questionId) {
                return null;
            }

            @Override
            public QuestionEditRequestVM getQuestionEditRequestVM(Question question) {
                return null;
            }

            @Override
            public Integer selectAllCount() {
                return null;
            }

            @Override
            public List<Integer> selectMothCount() {
                return null;
            }

            @Override
            public int deleteById(Integer id) {
                return 0;
            }

            @Override
            public int insert(Question record) {
                return 0;
            }

            @Override
            public int insertByFilter(Question record) {
                return 0;
            }

            @Override
            public Question selectById(Integer id) {
                return null;
            }

            @Override
            public int updateByIdFilter(Question record) {
                return 0;
            }

            @Override
            public int updateById(Question record) {
                return 0;
            }
        };
        this.textContentService = new TextContentService() {
            @Override
            public <T, R> TextContent jsonConvertInsert(List<T> list, Date now, Function<? super T, ? extends R> mapper) {
                return null;
            }

            @Override
            public <T, R> TextContent jsonConvertUpdate(TextContent textContent, List<T> list, Function<? super T, ? extends R> mapper) {
                return null;
            }

            @Override
            public int deleteById(Integer id) {
                return 0;
            }

            @Override
            public int insert(TextContent record) {
                return 0;
            }

            @Override
            public int insertByFilter(TextContent record) {
                return 0;
            }

            @Override
            public TextContent selectById(Integer id) {
                return null;
            }

            @Override
            public int updateByIdFilter(TextContent record) {
                return 0;
            }

            @Override
            public int updateById(TextContent record) {
                return 0;
            }
        };
    }

    @RequestMapping(value = "/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<QuestionResponseVM>> pageList(@RequestBody QuestionPageRequestVM model) {
        PageInfo<Question> pageInfo = questionService.page(model);
        PageInfo<QuestionResponseVM> page = PageInfoHelper.copyMap(pageInfo, q -> {
            QuestionResponseVM vm = modelMapper.map(q, QuestionResponseVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(q.getCreateTime()));
            vm.setScore(ExamUtil.scoreToVM(q.getScore()));
            TextContent textContent = textContentService.selectById(q.getInfoTextContentId());
            QuestionObject questionObject = JsonUtil.toJsonObject(textContent.getContent(), QuestionObject.class);
            String clearHtml = HtmlUtil.clear(questionObject.getTitleContent());
            vm.setShortTitle(clearHtml);
            return vm;
        });
        return RestResponse.ok(page);
    }
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public RestResponse edit(@RequestBody @Valid QuestionEditRequestVM model) {
        RestResponse validQuestionEditRequestResult = validQuestionEditRequestVM(model);
        if (validQuestionEditRequestResult.getCode() != SystemCode.OK.getCode()) {
            return validQuestionEditRequestResult;
        }
        if (null == model.getId()) {
            questionService.insertFullQuestion(model, getCurrentUser().getId());
        } else {
            questionService.updateFullQuestion(model);
        }
        return RestResponse.ok();
    }
    @RequestMapping(value = "/select/{id}", method = RequestMethod.POST)
    public RestResponse<QuestionEditRequestVM> select(@PathVariable Integer id) {
        QuestionEditRequestVM newVM = questionService.getQuestionEditRequestVM(id);
        return RestResponse.ok(newVM);
    }
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    public RestResponse delete(@PathVariable Integer id) {
        Question question = questionService.selectById(id);
        question.setDeleted(true);
        questionService.updateByIdFilter(question);
        return RestResponse.ok();
    }

    // 获取字符串类型的单元格内容
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return ""; // 如果单元格为空，返回空字符串
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return ""; // 其他类型返回空字符串
        }
    }

    // 获取数字类型的单元格内容
    private double getNumericCellValue(Cell cell) {
        if (cell == null) {
            return 0; // 如果单元格为空，返回0
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0; // 如果无法转换为数字，返回0
                }
            default:
                return 0; // 其他类型返回0
        }
    }

    // 导入题库部分
    @PostMapping(value = "/uploadQuestionBank")
    public ResponseEntity<String> uploadQuestionBank(@RequestParam("file") MultipartFile file) throws SQLException {
        // 文件第一列“题目类型”，第二列“分数”，第三列“难度”，第四列“答案”，第五列“知识点”，第六列“内容”，第一列是（1~5的数字），第三列是（1~3）的难度
        System.out.println("我来到导入题库部分了");
        List<QuestionBank> questionBankList = new ArrayList<>();

        try {
            // 获取 Excel 文件的输入流
            InputStream inputStream = file.getInputStream();

            // 使用 Apache POI 解析 Excel 文件
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表

            // 遍历每一行
            // 遍历每一行
            for (Row row : sheet) {
                // 假设第一行是表头，从第二行开始
                if (row.getRowNum() == 0) {
                    continue; // 跳过表头
                }

                // 获取每一列的内容，先检查单元格是否为空
                String questionType = getCellValue(row.getCell(0));   // 题目类型
                double score = getNumericCellValue(row.getCell(1));    // 分数
                String difficulty = getCellValue(row.getCell(2));      // 难度
                String answer = getCellValue(row.getCell(3));          // 答案
                String knowledgePoint = getCellValue(row.getCell(4));  // 知识点
                String content = getCellValue(row.getCell(5));         // 内容

                // 创建 Question 对象并加入列表
                QuestionBank question = new QuestionBank(questionType, score, difficulty, answer, knowledgePoint, content);
                questionBankList.add(question);

                // System.out.println(question.toString());
            }



            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("文件读取失败");
        }

        System.out.println("");
        // 郭百乐的题库去重代码
        List<QuestionBank> gblQuChong = EstimateRepetition(questionBankList,this.getBank());

        for (QuestionBank q : gblQuChong){
            System.out.println(q.toString());
        }
        System.out.println("共有" + gblQuChong.size() + "道题目");

        // 高梓翼的代码（）

        return null;
    }

    // 郭百乐的代码（从题库中去重的代码）  List<QuestionBank> questionBankList = new ArrayList<>();
    public List<QuestionBank> EstimateRepetition(List<QuestionBank> questionBankList, List<QuestionBank> banktopicList)
    {
        //题库中的关键词
        Map<Set<String>, List<QuestionBank>> bandKeyWordList = getKeyWordList(banktopicList);

        questionBankList = removeDuplicatesInTopicList(questionBankList);

        //新导入题库的关键词
        Map<Set<String>, List<QuestionBank>> keyWordList = getKeyWordList(questionBankList);


        //用于存储重复的题
        List<QuestionBank> repetitionQuestion = new ArrayList<>();

        // 遍历map1
        for (Map.Entry<Set<String>, List<QuestionBank>> entry1 : bandKeyWordList.entrySet()) {
            Set<String> key1 = entry1.getKey();
            List<QuestionBank> questionBank1 = entry1.getValue();
            if (questionBank1 == null) continue; // 确保topic1不为null
            for (QuestionBank bank : questionBank1) {
                //得到对应关键词的题
                String value1 = bank.getContent();
                // 遍历map2
                for (Map.Entry<Set<String>,List<QuestionBank>> entry2 : keyWordList.entrySet()) {
                    Set<String> key2 = entry2.getKey();
                    List<QuestionBank> questionBank2 = entry2.getValue();
                    if (questionBank2 == null) continue; // 确保topic2不为null
                    for (QuestionBank questionBank : questionBank2) {
                        String value2 = questionBank.getContent();
                        // 先比较键（Set<String>）是否内容完全一致（忽略顺序）
                        if (areSetsEqual(key1, key2)) {
                            // 再比较值（String）是否相等
                            if (ifSameTopic(value1,value2)) {
                                repetitionQuestion.add(questionBank);
                                break;
                            }
                        }
                    }

                }
            }

        }

        // 从keyWordList中删除重复的元素
        Iterator<QuestionBank> iterator = questionBankList.iterator();
        while (iterator.hasNext()) {
            QuestionBank questionBank = iterator.next();
            if (repetitionQuestion.contains(questionBank)) {
                iterator.remove();
            }
        }

        return questionBankList;

    }


    // 对topicList本身进行去重的方法（基于题目整体是否重复进行判断）
    private List<QuestionBank> removeDuplicatesInTopicList(List<QuestionBank> questionBankList) {
        Set<QuestionBank> uniqueQuestionBanks = new HashSet<>();
        List<QuestionBank> result = new ArrayList<>();
        for (QuestionBank questionBank : questionBankList) {
            if (uniqueQuestionBanks.add(questionBank)) {
                // 如果添加成功，说明是不重复的元素，加入结果列表
                result.add(questionBank);
            }
        }
        return result;
    }

    // 判断两个Set<String>内容是否完全一致（忽略顺序）
    private static boolean areSetsEqual(Set<String> set1, Set<String> set2) {
        return set1.size() == set2.size() && set1.containsAll(set2);
    }


    //将题库的关键词与题干存入Set集合
    public Map<Set<String>, List<QuestionBank>> getKeyWordList(List<QuestionBank> questionList) {
        Map<Set<String>, List<QuestionBank>> keyWordList = new LinkedHashMap<>();
        for (QuestionBank questionBank : questionList) {
            // 先做null判断，避免空指针异常
            if (questionBank.getKnowledgePoint()!= null) {
                Set<String> keywords = getKeyWord(questionBank.getKnowledgePoint());
                // 从map中获取已有的对应关键词的QuestionBank列表，如果不存在则创建一个新的空列表
                List<QuestionBank> questionBankList = keyWordList.getOrDefault(keywords, new ArrayList<>());
                questionBankList.add(questionBank);
                keyWordList.put(keywords, questionBankList);
            } else {
                // 创建一个特殊的标识集合来表示对应topic的关键词为空的情况
                Set<String> nullKeywords = Collections.singleton("NULL");
                // 从map中获取已有的对应关键词（NULL情况）的QuestionBank列表，如果不存在则创建一个新的空列表
                List<QuestionBank> questionBankList = keyWordList.getOrDefault(nullKeywords, new ArrayList<>());
                questionBankList.add(questionBank);
                keyWordList.put(nullKeywords, questionBankList);
            }
        }
        return keyWordList;
    }



    //将string字符串，提取分词
    public Set<String> getKeyWord(String text) {
        String textWithoutPunctuation = removePunctuation(text);
        // 再使用JiebaSegmenter进行分词
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> segments = segmenter.sentenceProcess(textWithoutPunctuation);
        Set<String> result = new HashSet<String>();
        for (String segment : segments) {
            if (segment!= null && segment.length() > 0) {
                result.add(segment);
            }
        }
        return result;
    }
    public String removePunctuation(String text) {
        // 常见的标点符号的Unicode编码范围（这只是涵盖了一部分常见的标点，实际标点种类繁多）
        String punctuationRegex = "[\\p{Punct}]";
        return text.replaceAll(punctuationRegex, "");
    }

    //重复题目判断
    public boolean ifSameTopic(String question1,String question2)
    {
        Set<String> keywords1 = getKeyWord(question1);
        Set<String> keywords2 = getKeyWord(question2);

        // 找出两个集合中相同的字符串，存入新的集合
        Set<String> commonKeywords = new HashSet<>();
        for (String s : keywords1) {
            if (keywords2.contains(s)) {
                commonKeywords.add(s);
            }
        }
        // 将keywords1的长度与重复字符串长度比较，如果长度超过keywords1的0.9，则返回true，否则返回false
        double threshold = 0.9;
        double ratio = (double) commonKeywords.size() / keywords1.size();
        return ratio >= threshold;

    }

    // 郭百乐的获取数据库题目代码
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public List<QuestionBank> getBank() throws SQLException {
        List<QuestionBank> questionBanks = new ArrayList<>();
        try {
            Connection connection = getConnection();
            if (connection!= null) {
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT  xzs.t_question.id,  xzs.t_question.knowledge_points,  xzs.t_text_content.content " +
                        "FROM  xzs.t_question " +
                        "JOIN  xzs.t_text_content ON  xzs.t_question.info_text_content_id =  xzs.t_text_content.id");
                ObjectMapper objectMapper = new ObjectMapper();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String knowledgePoint = resultSet.getString("knowledge_points");
                    String jsonContent = resultSet.getString("content");
                    QuestionBank questionBank = parseQuestionBank(id, knowledgePoint, jsonContent, objectMapper);
                    questionBanks.add(questionBank);
                }
                resultSet.close();
                statement.close();
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questionBanks;
    }

    public QuestionBank parseQuestionBank(int id, String knowledgePoint, String jsonContent, ObjectMapper objectMapper) {
        QuestionBank questionBank = new QuestionBank(id, knowledgePoint, "");
        try {
            // 将JSON字符串解析为Map
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);
            String titleContent = (String) jsonMap.getOrDefault("titleContent", "").toString();
            String analyze = (String) jsonMap.getOrDefault("analyze", "").toString();
            List<String> questionItemContents = new ArrayList<>();
            if (jsonMap.containsKey("questionItemObjects")) {
                List<Map<String, Object>> itemList = (List<Map<String, Object>>) jsonMap.get("questionItemObjects");
                for (Map<String, Object> itemMap : itemList) {
                    questionItemContents.add((String) itemMap.getOrDefault("content", ""));
                }
            }
            String correct = (String) jsonMap.getOrDefault("correct", "").toString();
            String newContent =  titleContent +  analyze  + questionItemContents  + correct;
            questionBank.setContent(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questionBank;
    }




    public class QuestionBank {
        public QuestionBank(int id, String knowledgePoint, String content) {
            this.id = id;
            this.content = content;
            this.knowledgePoint = knowledgePoint;
        }

        int id; // 郭百乐的垃圾代码
        private String questionType;   // 题目类型
        private double score;          // 分数
        private String difficulty;     // 难度
        private String answer;         // 答案
        private String knowledgePoint; // 知识点
        private String content;        // 内容

        // 构造方法
        public QuestionBank(String questionType, double score, String difficulty, String answer, String knowledgePoint, String content) {
            this.questionType = questionType;
            this.score = score;
            this.difficulty = difficulty;
            this.answer = answer;
            this.knowledgePoint = knowledgePoint;
            this.content = content;
        }

        // Getter 和 Setter 方法
        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getKnowledgePoint() {
            return knowledgePoint;
        }

        public void setKnowledgePoint(String knowledgePoint) {
            this.knowledgePoint = knowledgePoint;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        // toString 方法，用于打印题目的信息
        @Override
        public String toString() {
            return "Question{" +
                    "questionType='" + questionType + '\'' +
                    ", score=" + score +
                    ", difficulty='" + difficulty + '\'' +
                    ", answer='" + answer + '\'' +
                    ", knowledgePoint='" + knowledgePoint + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }


    private RestResponse validQuestionEditRequestVM(QuestionEditRequestVM model) {
        int qType = model.getQuestionType().intValue();
        boolean requireCorrect = qType == QuestionTypeEnum.SingleChoice.getCode() || qType == QuestionTypeEnum.TrueFalse.getCode();
        if (requireCorrect) {
            if (StringUtils.isBlank(model.getCorrect())) {
                String errorMsg = ErrorUtil.parameterErrorFormat("correct", "不能为空");
                return new RestResponse<>(SystemCode.ParameterValidError.getCode(), errorMsg);
            }
        }
        if (qType == QuestionTypeEnum.GapFilling.getCode()) {
            Integer fillSumScore = model.getItems().stream().mapToInt(d -> ExamUtil.scoreFromVM(d.getScore())).sum();
            Integer questionScore = ExamUtil.scoreFromVM(model.getScore());
            if (!fillSumScore.equals(questionScore)) {
                String errorMsg = ErrorUtil.parameterErrorFormat("score", "空分数和与题目总分不相等");
                return new RestResponse<>(SystemCode.ParameterValidError.getCode(), errorMsg);
            }
        }
        return RestResponse.ok();
    }

//    public void main(String[] args) throws SQLException {
//        List<QuestionBank> List = this.getBank();
//        for (QuestionBank questionBank : List) {
//            System.out.println(questionBank.toString());
//        }
//    }

//    public static void main(String[] args) throws SQLException {
//        for (QuestionBank questionBank : new QuestionController().getBank()) {
//            System.out.println(questionBank.toString());
//        }
//    }

}
