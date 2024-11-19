package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.service.FaceRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionServiceImpl.class);

    @Override
    public String recognizeFace(String imageData) {
        String line = "";
        String ret = "";
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "conda activate detector && cd D:\\python-learn\\detect && python test.py"
            );
            Process p = pb.start();

            // 将 imageData 通过输出流传递给 Python 脚本
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
            writer.write(imageData);
            writer.newLine();
            writer.flush();
            writer.close();

            // 从 Python 脚本中读取输出，使用 UTF-8 编码
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            while ((line = in.readLine()) != null) {
                System.out.println("从python脚本中读出" + line);
                ret = line;
            }

            // 从 Python 脚本中读取错误输出，使用 UTF-8 编码
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream(), "UTF-8"));
            while ((line = err.readLine()) != null) {
                System.err.println(line);
            }

            // 等待 Python 脚本运行结束
            int exitCode = p.waitFor();
            System.out.println("Python script exited with code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("here is ret :" + ret);
        return ret;
    }
}
