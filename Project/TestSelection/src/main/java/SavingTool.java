import java.io.FileWriter;
import java.io.IOException;

/**
 * 将内容输出保存成文件
 */
public class SavingTool {
    static void outputFile(String path,String content) throws IOException {
        FileWriter fileWriter = new FileWriter(path);
        fileWriter.write(content);
        fileWriter.flush();
        fileWriter.close();
    }
}
