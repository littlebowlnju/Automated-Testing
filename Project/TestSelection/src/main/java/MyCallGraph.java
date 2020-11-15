import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自己构建的调用图,方便更多操作
 */
public class MyCallGraph {
    // key为方法名，value为调用了该方法的方法名集合
    public Map<String, Set<String>> graph = new HashMap<>();

    static char methodMode = 'm';
    static char classMode = 'c';

    // 往图中添加节点（如果不在图中的话）
    public void addNode(String methodName){
        if(!graph.containsKey(methodName)){
            graph.put(methodName,new HashSet<>());
        }
    }

    // 往图中添加边（即调用关系），src是被调用方法
    public void addEdge(String src,String tar){
        addNode(src);
        addNode(tar);
        graph.get(src).add(tar);
    }

    // 根据调用图选择测试用例
    // changed为更改信息集合
    // mode为选择的粒度，'c'为类级，'m'为方法级
    public Set<String> selectTest(Set<String> changed,char mode) {
        Set<String> callees = new HashSet<>();
        if(mode == classMode){
            Set<String> changedClasses = new HashSet<>();
            for(String method : changed){
                changedClasses.add(method.split(" ")[0]);//选择修改的类
            }

            for(String methodName : graph.keySet()){
                if(changedClasses.contains(methodName.split(" ")[0])){
                    callees.add(methodName);
                }
            }
        }else if(mode == methodMode){
            callees.addAll(changed);
        }
        //从图中寻找调用了callees的方法,以及更深层次的调用关系，直到集合不再变化
        while(true){
            Set<String> callers = new HashSet<>(callees);
            for(String callee : callees){
                callers.addAll(graph.get(callee));
            }
            if(callees.size()==callers.size()){
                break;
            }
            callees = callers;
        }

        Set<String> selectedTests = new HashSet<>();
        for(String callee : callees){
            if(Test.isTest(callee.split(" ")[0])&&!callee.contains("<init>")){
                selectedTests.add(callee);
            }
        }
        return selectedTests;
    }
}
