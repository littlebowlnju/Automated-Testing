import java.util.HashSet;
import java.util.Set;

/**
 * 用于存储测试和判断一个类是否为测试类
 */
public class Test {
    static Set<String> tests = new HashSet<>();

    //存储测试类
    public static void addTest(String className){
        tests.add("L"+className);
    }

    //判断一个类是否为测试类
    public static boolean isTest(String className){
        return tests.contains(className);
    }

    //用于调试
    public static Set<String> getTests(){
        return tests;
    }
}
