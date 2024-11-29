import java.io.*;
import java.util.*;
import com.google.gson.Gson;
public class JavaToPythonCaller {
    public static void main(String[] args) {
        // 创建两个字符串列表
        List<String> list1 = Arrays.asList("apple", "banana", "cherry");
        List<String> list2 = Arrays.asList("dog", "elephant", "fox");
        // 调用 Python 脚本，并传递两个列表
        try {
            // 运行 Python 脚本
            ProcessBuilder pb = new ProcessBuilder("python", "testString.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // 传递列表到 Python 脚本
            OutputStream os = process.getOutputStream();
            PrintWriter writer = new PrintWriter(os);
            // 使用 Gson 库将 Java 列表转换为 JSON 字符串
            Gson gson = new Gson();
            writer.println(gson.toJson(list1)); // 将列表1转换为 JSON 字符串并传递给 Python
            writer.println(gson.toJson(list2)); // 将列表2转换为 JSON 字符串并传递给 Python
            writer.flush();
            // 获取 Python 脚本的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line);
            }
            // 获取 Python 脚本的返回值
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python script executed successfully.");
            } else {
                System.out.println("Python script failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
