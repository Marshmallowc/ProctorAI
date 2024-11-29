package com.mindskip.xzs.controller.admin;
import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.other.KeyValue;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.domain.UserEventLog;
import com.mindskip.xzs.domain.enums.UserStatusEnum;
import com.mindskip.xzs.service.AuthenticationService;
import com.mindskip.xzs.service.UserEventLogService;
import com.mindskip.xzs.service.UserService;
import com.mindskip.xzs.utility.DateTimeUtil;
import com.mindskip.xzs.viewmodel.admin.user.*;
import com.mindskip.xzs.utility.PageInfoHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
@RestController("AdminUserController")
@RequestMapping(value = "/api/admin/user")
public class UserController extends BaseApiController {
    @PersistenceContext
    private EntityManager entityManager;
    private final UserService userService;
    private final UserEventLogService userEventLogService;
    private final AuthenticationService authenticationService;
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/xzs?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    @Autowired
    public UserController(UserService userService, UserEventLogService userEventLogService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.userEventLogService = userEventLogService;
        this.authenticationService = authenticationService;
    }
    @RequestMapping(value = "/page/list", method = RequestMethod.POST)
    public RestResponse<PageInfo<UserResponseVM>> pageList(@RequestBody UserPageRequestVM model) {
        System.out.println("UserController's /page/list");
        System.out.println("model");
        System.out.println(model);
        System.out.println(model.getUserName() + " eeeee " + model.getUserName().length());
        System.out.println(model.getRole());
        System.out.println("55555555555555555555");
        PageInfo<User> pageInfo = userService.userPage(model);
        PageInfo<UserResponseVM> page = PageInfoHelper.copyMap(pageInfo, d -> UserResponseVM.from(d));
        System.out.println("pageInfo");
        System.out.println(pageInfo);
        System.out.println(pageInfo.getSize());
        System.out.println(page.getList().get(0).getRealName());
        System.out.println(page.getList().get(0).getSchool());
        // 对用户进行排序，将排序之后的结果再发给前端
        Collections.sort(page.getList(), new Comparator<UserResponseVM>() {
            @Override
            public int compare(UserResponseVM u1, UserResponseVM u2) {
                // 首先比较学校名称
                int schoolCompare = u1.getSchool().compareTo(u2.getSchool());
                if (schoolCompare != 0) {
                    return schoolCompare;
                }
                // 如果学校名称相同，比较学号
                return u1.getStuNo().compareTo(u2.getStuNo());
            }
        });
        return RestResponse.ok(page);
    }
    @PostMapping("/downloadFace")
    public ResponseEntity<String> importStudents(@RequestParam("file") MultipartFile file) {
        System.out.println("我来到downloadFace了");
        // 检查文件是否为空
        System.out.println("检查文件是否为空");
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }
        // 检查文件类型（例如，只允许图片格式）
        System.out.println("检查文件类型");
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("请上传有效的图片文件");
        }
        // 检查文件大小（例如，限制在5MB以内）
        System.out.println("检查文件大小");
        long fileSize = file.getSize();
        if (fileSize > 5 * 1024 * 1024) { // 5MB
            return ResponseEntity.badRequest().body("文件大小不能超过5MB");
        }
        // 尝试读取图片
        System.out.println("尝试读取图片");
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ResponseEntity.badRequest().body("无效的图片文件");
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("文件读取失败");
        }
        // 处理文件（例如，保存到服务器等）
        // ...
        return ResponseEntity.ok("文件上传成功");
    }
    @RequestMapping(value = "/event/page/list", method = RequestMethod.POST)
    public RestResponse<PageInfo<UserEventLogVM>> eventPageList(@RequestBody UserEventPageRequestVM model) {
        System.out.println("UserController's /event/page/list");
        PageInfo<UserEventLog> pageInfo = userEventLogService.page(model);
        PageInfo<UserEventLogVM> page = PageInfoHelper.copyMap(pageInfo, d -> {
            UserEventLogVM vm = modelMapper.map(d, UserEventLogVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(d.getCreateTime()));
            return vm;
        });
        return RestResponse.ok(page);
    }
    // 获取t_face_encoding表中的数据
    @RequestMapping(value = "/getFaceList", method = RequestMethod.POST)
    public List<Map<String, Object>> getFaceList() {
        System.out.println("准备进行人脸获取");
        List<Map<String, Object>> faceEncodingList = new ArrayList<>();
        // JDBC连接和查询操作
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            // 执行SQL查询
            String sql = "SELECT id, face_name, encoding, stuNo FROM t_face_encoding";
            ResultSet resultSet = statement.executeQuery(sql);
            // 处理查询结果
            while (resultSet.next()) {
                Map<String, Object> faceEncoding = new HashMap<>();
                faceEncoding.put("id", resultSet.getInt("id"));
                faceEncoding.put("faceName", resultSet.getString("face_name"));
                if(resultSet.getString("encoding").isEmpty()){
                faceEncoding.put("encoding", "None");
                } else {
                    faceEncoding.put("encoding", "Exist");
                }
                System.out.println("resultSet = " + resultSet.getString("stuNo"));
                if(resultSet.getString("stuNo") == null){
                    faceEncoding.put("stuNo", "None");
                } else {
                    faceEncoding.put("stuNo", resultSet.getString("stuNo"));
                }
                faceEncodingList.add(faceEncoding);
            }
        } catch (SQLException e) {
            e.printStackTrace();  // 错误处理
        }
        return faceEncodingList;  // 返回结果列表
    }
    @RequestMapping(value = "/select/{id}", method = RequestMethod.POST)
    public RestResponse<UserResponseVM> select(@PathVariable Integer id) {
        System.out.println("UserController's select/{id}");
        System.out.println("select/{id}");
        User user = userService.getUserById(id);
        UserResponseVM userVm = UserResponseVM.from(user);
        return RestResponse.ok(userVm);
    }
    @RequestMapping(value = "/current", method = RequestMethod.POST)
    public RestResponse<UserResponseVM> current() {
        System.out.println("UserController's current");
        User user = getCurrentUser();
        UserResponseVM userVm = UserResponseVM.from(user);
        return RestResponse.ok(userVm);
    }
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public RestResponse<User> edit(@RequestBody @Valid UserCreateVM model) throws SQLException {
        System.out.println("UserController's edit");
        if (model.getId() == null) {  //create
            User existUser = userService.getUserByUserName(model.getUserName());
            if (null != existUser) {
                return new RestResponse<>(2, "用户已存在");
            }
            if (StringUtils.isBlank(model.getPassword())) {
                return new RestResponse<>(3, "密码不能为空");
            }
        }
        if (StringUtils.isBlank(model.getBirthDay())) {
            model.setBirthDay(null);
        }
        User user = modelMapper.map(model, User.class);
        if (model.getId() == null) {
            String encodePwd = authenticationService.pwdEncode(model.getPassword());
            user.setPassword(encodePwd);
            user.setUserUuid(UUID.randomUUID().toString());
            user.setCreateTime(new Date());
            user.setLastActiveTime(new Date());
            user.setDeleted(false);
            userService.insertByFilter(user);
        } else {
            if (!StringUtils.isBlank(model.getPassword())) {
                String encodePwd = authenticationService.pwdEncode(model.getPassword());
                user.setPassword(encodePwd);
            }
            user.setModifyTime(new Date());
            userService.updateByIdFilter(user);
        }
        System.out.println("这里是edit中的user内容" + user.getUserName());
        System.out.println("这里是edit中的user内容" + user.getStuNo());
        System.out.println("这里是edit中的user内容" + user);
        String stuNo = user.getStuNo();
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
            // 检查班级是否存在
            ResultSet rs = stmt.executeQuery("SELECT class_name FROM class WHERE class_name = '" + classRoom + "'");
            if (!rs.next()) {
                // 如果班级不存在，则添加
                String insertSQL = "INSERT INTO class (class_name) VALUES ('" + classRoom + "')";
                String insertClassNoSQL = "INSERT INTO class (class_no) VALUES ('" + classRoom.substring(4) + "')";
                String insertCollegeNoSQL = "INSERT INTO class (college_no) VALUES ('" + classRoom.substring(0, 4) + "')";
                String insertMajorNoSQL = "INSERT INTO class (major_no) VALUES ('" + classRoom.substring(0, 5) + "')";
                String insertGradeNoSQL = "INSERT INTO class (grade_no) VALUES ('" + classRoom.substring(0, 2) + "')";
                stmt.executeUpdate(insertSQL);
                stmt.executeUpdate(insertClassNoSQL);
                stmt.executeUpdate(insertCollegeNoSQL);
                stmt.executeUpdate(insertMajorNoSQL);
                stmt.executeUpdate(insertGradeNoSQL);
                System.out.println("班级 " + classRoom + " 已添加到数据库.");
            } else {
                System.out.println("班级 " + classRoom + " 已存在.");
            }
        }
        return RestResponse.ok(user);
    }
    @Entity
    private class Classinfo {
        @Id
        private Long id; // 主键字段
        private String className;
        private String schoolId;
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getClassName() {
            return className;
        }
        public void setClassName(String className) {
            this.className = className;
        }
        public String getSchoolId() {
            return schoolId;
        }
        public void setSchoolId(String schoolId) {
            this.schoolId = schoolId;
        }
    }
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public RestResponse update(@RequestBody @Valid UserUpdateVM model) {
        System.out.println("UserController's update");
        User user = userService.selectById(getCurrentUser().getId());
        modelMapper.map(model, user);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok();
    }
    @RequestMapping(value = "/changeStatus/{id}", method = RequestMethod.POST)
    public RestResponse<Integer> changeStatus(@PathVariable Integer id) {
        System.out.println("UserController's changeStatus/{id}");
        User user = userService.getUserById(id);
        UserStatusEnum userStatusEnum = UserStatusEnum.fromCode(user.getStatus());
        Integer newStatus = userStatusEnum == UserStatusEnum.Enable ? UserStatusEnum.Disable.getCode() : UserStatusEnum.Enable.getCode();
        user.setStatus(newStatus);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok(newStatus);
    }
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    public RestResponse delete(@PathVariable Integer id) {
        System.out.println("UserController's /delete/{id}");
        User user = userService.getUserById(id);
        user.setDeleted(true);
        userService.updateByIdFilter(user);
        return RestResponse.ok();
    }
    @RequestMapping(value = "/selectByUserName", method = RequestMethod.POST)
    public RestResponse<List<KeyValue>> selectByUserName(@RequestBody String userName) {
        System.out.println("UserController's /selectByUserName");
        List<KeyValue> keyValues = userService.selectByUserName(userName);
        return RestResponse.ok(keyValues);
    }
}
