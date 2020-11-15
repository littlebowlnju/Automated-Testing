import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 生成自己的Dot文件
 */
public class MyDot {
    String name;
    Set<String> dependencies = new HashSet<>();

    public MyDot(String name){
        this.name = name;
    }

    //添加边（依赖关系）
    public void addDependency(String src,String tar){
        this.dependencies.add("\t\""+src+"\" -> \""+tar+"\";");
    }

    public void saveToFile(String path)throws IOException{
        SavingTool.outputFile(path,"digraph "+name+"_class"+"{\n"+String.join("\n",dependencies)+"\n}\n");
    }
}
