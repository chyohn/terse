# 案例2：异步任务命令的使用——面向任务设计

## 案例描述
与[案例2：流程合并——面向流程设计](example_flow-oriented_merge_zh.md)一样，
实现算术表达：(A+B)*(C+D)+(C+D)/(E-F)，DAG图如下，为了简单表达，我们的数字都是整型。

[//]: # (![flow2.jpg]&#40;https://s21.ax1x.com/2024/05/19/pku7FUJ.jpg&#41;)
![flow2.jpg](./image/flow2.jpg)

## 实现方案

这篇案例我们复用[案例1：异步任务命令的使用——面向流程设计](./example_flow-oriented_command_zh.md)的计算器命令类和处理器代码，从而只需要单独定义流程即可，实现方案如下：
1. 定义6个任务类，代表6个节点，分别为H1Task、H2Task、H3Task、H4Task（依赖H1和H2）、H5Task（依赖H2和H3）、H6Task（依赖H4和H5）
   每个任务都是单命令，简单起见，我们的任务都实现ITaskSingle接口。
2. 定义一个Summary接口，依赖流程最后的节点H6，最后输出结果。
3. 所有任务都实现为单例，包括Summary。

## 代码实现


1. 定义H1任务
```java
class H1Task implements ITaskSingle<FlowContext, Integer> {
    public final static H1Task INSTANCE = new H1Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getA(), Op.ADD, context.getB());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH1(result);
    }
}
```

2. 定义H2任务
```java
class H2Task implements ITaskSingle<FlowContext, Integer> {
    public final static H2Task INSTANCE = new H2Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getC(), Op.ADD, context.getD());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH2(result);
    }
}
```

3. 定义H3任务
```java
class H3Task implements ITaskSingle<FlowContext, Integer> {
    public final static H3Task INSTANCE = new H3Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getE(), Op.SUBTRACT, context.getF());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH3(result);
    }
}
```

4. 定义H4任务
```java
class H4Task implements ITaskSingle<FlowContext, Integer> {
    public final static H4Task INSTANCE = new H4Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        List<ITask<FlowContext>> tasks = new ArrayList<>();
        tasks.add(H1Task.INSTANCE);
        tasks.add(H2Task.INSTANCE);
        return tasks;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getH1(), Op.MULTIPLY, context.getH2());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH4(result);
    }
}
```

5. 定义H5任务
```java
class H5Task implements ITaskSingle<FlowContext, Integer> {
    public final static H5Task INSTANCE = new H5Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        List<ITask<FlowContext>> tasks = new ArrayList<>();
        tasks.add(H2Task.INSTANCE);
        tasks.add(H3Task.INSTANCE);
        return tasks;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getH2(), Op.DIVIDE, context.getH3());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH5(result);
    }
}
```

6. 定义H6任务
```java
class H6Task implements ITaskSingle<FlowContext, Integer> {
    public final static H6Task INSTANCE = new H6Task();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        List<ITask<FlowContext>> tasks = new ArrayList<>();
        tasks.add(H4Task.INSTANCE);
        tasks.add(H5Task.INSTANCE);
        return tasks;
    }
    @Override
    public ICommandX<Integer> getCommand(FlowContext context) {
        return new CalculateCommand(context.getH4(), Op.ADD, context.getH5());
    }
    @Override
    public void handleResult(FlowContext context, Integer result) {
        context.setH6(result);
    }
}
```

7. 定义Summary任务
```java
class Summary implements ISummaryTask<FlowContext> {
    public final static Summary INSTANCE = new Summary();
    @Override
    public void summary(FlowContext context) {
        System.out.printf("(%s+%s)*(%s+%s)+(%s+%s)/(%s-%s) = %s \n", context.getA(), context.getB(),
            context.getC(), context.getD(),
            context.getC(), context.getD(), context.getE(), context.getF(),
            context.getH6());
    }
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        List<ITask<FlowContext>> tasks = new ArrayList<>();
        tasks.add(H6Task.INSTANCE);
        return tasks;
    }
}
```

8. 定义FlowContext
```java
@Setter
@Getter
private static class FlowContext implements IFlowContext {

    private int a;
    private int b;
    private int c;
    private int d;
    private int e;
    private int f;

    private int h1;
    private int h2;
    private int h3;
    private int h4;
    private int h5;
    private int h6;
}
```

9. 执行流程
```java
FlowContext context = new FlowContext();
context.setA(1);
context.setB(2);
context.setC(3);
context.setD(4);
context.setE(5);
context.setF(6);
Terse.execute(Summary.INSTANCE, context);
```
输出结果：
```
(1+2)*(3+4)+(3+4)/(5-6) = 14 
```

## 总结
通过上面的案例，我们可以看到，对每个任务单独定义并管理其依赖任务，可以很方便的实现一个流程，而不需要关心任务的执行顺序，
这样的设计可以很好的解耦任务之间的关系，提高代码的可维护性。

另外需要说明的是：上面的Task代码都在同一个线程中执行，因此没有线程安全问题，这个线程就是调用execute方法的线程。
而所有任务能够异步执行是因为任务提交的命令对应的命令处理器handler是异步执行的。
