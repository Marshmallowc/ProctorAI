package com.mindskip.xzs.controller.student;

import com.mindskip.xzs.service.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/face-recognition")
public class FaceRecognitionController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @PostMapping
    public ResponseEntity<Map<String, String>> recognizeFace(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();

        try {
            String imageData = request.get("image");
            if (imageData == null || imageData.isEmpty()) {
                response.put("result", "error");
                response.put("message", "No image data provided");
                return ResponseEntity.badRequest().body(response);
            }

            String result = faceRecognitionService.recognizeFace(imageData);
            System.out.println("result's value = " + result);
            response.put("result", result);

            // 处理其他可能的响应状态
            if (result.contains("success") || result.contains("failed")) {
                System.out.println(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
                return ResponseEntity.ok(response);
            } else {
                System.out.println("come to else");
                System.out.println(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("result", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
