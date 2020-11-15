import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class TestSelection {
    public static void main(String[] args){
        try {
            String mode = args[0]; // 模式，-c执行类级测试选择，-m执行方法及测试选择,-d表示需要生成dot文件，用于生成pdf依赖图
            String targetPath = args[1]; // 指向测试项目target目录路径
            String changeInfo = args[2]; // 修改信息文件路径

            File exclusionFile = new File("exclusion.txt");
            AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", exclusionFile, TestSelection.class.getClassLoader());

            addClassToScope(scope, Paths.get(targetPath,"test-classes").toString());

            //存储测试类，便于之后挑选测试
            for(Module module : scope.getModules(ClassLoaderReference.Application)){
                if(module instanceof ClassFileModule){
                    Test.addTest(((ClassFileModule) module).getClassName());
                }
            }
            //System.out.println(Test.getTests());

            addClassToScope(scope,Paths.get(targetPath,"classes").toString());

            //生成类层次关系对象
            ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

            //生成进入点
            Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope,cha);

            //利用CHA算法构建调用图
            CHACallGraph cg = new CHACallGraph(cha);
            cg.init(eps);

            //生成自己的dot文件
            MyDot classDot = new MyDot("class");
            MyDot methodDot = new MyDot("method");

            //生成自己的调用图
            MyCallGraph callGraph = new MyCallGraph();

            //遍历cg中所有的节点
            for(CGNode node:cg){
                if(node.getMethod() instanceof ShrikeBTMethod){
                    IMethod method = node.getMethod();
                    if(method.getDeclaringClass().getClassLoader().toString().equals("Application")){
                        //获取声明该方法的类的内部表示
                        String classInnerName = method.getDeclaringClass().getName().toString();
                        String signature = method.getSignature();
                        String methodName = classInnerName + " " + signature;
                        //System.out.println(methodName);

                        //遍历调用该方法的方法
                        for(CGNode preNode : Iterator2Iterable.make(cg.getPredNodes(node))){
                            if(neededNode(preNode)){
                                IMethod preMethod = preNode.getMethod();
                                String preClassInnerName = preMethod.getDeclaringClass().getName().toString();
                                String preSignature = preMethod.getSignature();
                                String preMethodName = preClassInnerName + " " + preSignature;

                                if(mode.equals("-d")){
                                    classDot.addDependency(classInnerName,preClassInnerName);
                                    methodDot.addDependency(signature,preSignature);
                                }

                                callGraph.addEdge(methodName,preMethodName);
                            }
                        }
                    }
                }else{
                    System.out.println(String.format("'%s'不是一个ShrikeBTMethod:%s",node.getMethod(),node.getMethod().getClass()));
                }
            }

            if(mode.equals("-d")){
                //生成dot文件，后用graphviz生成依赖图
                classDot.saveToFile("class-cfa.dot");
                methodDot.saveToFile("method-cfa.dot");
            }

            //处理代码变更
            Set<String> changed = new HashSet<>();

            //按行读取
            FileInputStream inputStream = new FileInputStream(changeInfo);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            while((str=bufferedReader.readLine())!=null){
                changed.add(str);
            }
            //System.out.println(changed);
            bufferedReader.close();
            inputStream.close();

            //判断输出的模式
            //类级粒度输出
            if(mode.equals("-c")){
                //System.out.println(callGraph.selectTest(changed,'c'));
                SavingTool.outputFile("selection-class.txt",String.join("\n",callGraph.selectTest(changed,'c')));
            }
            //方法级粒度输出
            if(mode.equals("-m")){
                SavingTool.outputFile("selection-method.txt",String.join("\n",callGraph.selectTest(changed,'m')));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //添加要分析的class文件到分析域中
    static void addClassToScope(AnalysisScope scope,String path) throws InvalidClassFileException {
        File[] classes = new File(path).listFiles();
        if(classes!=null){
            for(File cl:classes){
                String clPath = cl.getAbsolutePath();
                if(cl.isDirectory()){
                    addClassToScope(scope, clPath);
                }else if(cl.getName().endsWith(".class")){
                    scope.addClassFileToScope(ClassLoaderReference.Application,new File(clPath));
                }
            }
        }
    }

    //判断是否为需要的节点
    static boolean neededNode(CGNode node){
        return (node.getMethod() instanceof ShrikeBTMethod) && (node.getMethod().getDeclaringClass().getClassLoader().toString().equals("Application"));
    }
}
