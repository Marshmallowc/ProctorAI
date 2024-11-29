import java.io.BufferedReader;
import java.io.InputStreamReader;
public class test {
    public static void main(String[] args) {
        try {
            // 创建一个 ProcessBuilder 来调用 Python 脚本
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "conda activate detector && cd D:\\python-learn\\detect && python test.py"
            );
            Process p = pb.start();
            // 从 Python 脚本中读取输出
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            // 从 Python 脚本中读取错误输出
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = err.readLine()) != null) {
                System.err.println(line);
            }
            // 等待 Python 脚本运行结束
            int exitCode = p.waitFor();
            System.out.println("Python script exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
