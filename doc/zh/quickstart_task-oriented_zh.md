# 快速开始——面向任务设计
## 一、面向任务设计介绍

面向任务设计可以轻松应对复杂流程的定义，该方式为分治思想，即把流程定义的关注点转移到各个任务上，通过任务的定义来完成流程的定义。

### 1. 需要实现的接口

接口名| 说明 
-|-
IFlowConext | 存储任务参数和任务结果
ITask | 定义任务
ISummaryTask | 汇总流程中最后的任务节点。
ICondtionTask | 定义条件任务，表示某个任务的执行需要满足一定条件才能执行

ITask、ISummaryTask、IConditionTask都是继承了接口IReliableTask，该接口主要定义了4个方法用于表示任务之间的依赖关系，如下表：

方法名 | 说明
-|-
mustRelyOnTasks() | 返回一个任务列表，表示列表中所有任务都完成后，当前任务才可以执行
randomRelyOnTasks() | 返回一个任务列表，表示列表中只要有一个任务完成，当前任务就可以执行
mustConditions() | 返回一个条件任务列表，表示列表中所有条件都满足后，当前任务才可以执行
randomConditions() | 返回一个条件任务列表，表示列表中任意一个条件满足时，当前任务就可以执行

### 2. 其他辅助接口

接口名| 说明 
-|-
ITaskSingle | 扩展ITask接口，表示任务只发起一个命令
ITaskMutiply | 扩展ITask接口，表示任务发起多个命令
ITaskHandler | 定义任务发起的命令和结果怎么处理，不建议直接实现，推荐使用TaskHandlerFactory提供的工厂方法或者任务直接实现ITaskSingle或ITaskMutiply。


### 3. 执行任务入口方法

直接使用Terse提供的静态方法Terse.execute(summary, context)。


### 4. 完整定义过程
1. 实现IFlowContext，定义流程参数数据
2. 每个任务实现ITask接口，确定任务所依赖的其它任何。另外，
   - 如果直接实现ITask接口，可通过TaskHandlerFactory来创建ITaskHandler对象
   - 也可直接实现ITask接口的扩展接口，提供单个命令的ITaskSingle或提供多个命令的ITaskMultiply
3. 实现ISummaryTask接口结束流程定义，其实现类只需依赖流程中最后的节点。

如果基于命令模式异步执行任务，还要完成后面3步:

4. 实现ICommandX<ReturnType>
5. 实现IReceiverFactory<MyCommandX>处理接收到的命令
6. 注册IReceiverFactory实现类，有两种方式，这里使用其中一种作为示例
```java
Terse.registerReceiverFactory(new MyReceiverFactory());
```


## 二、示例
以算术表达式 (A+B)*(C+D)/(E-F)为例，DAG图如下

[//]: # (![flow1.jpg]&#40;https://s21.ax1x.com/2024/05/19/pku7iE4.jpg&#41;)
![flow1.jpg](./image/flow1.jpg)

本示例中以创建[**线程池任务**]为例做演示，若要看[**异步任务命令**]的演示见[2.2 案例2：异步任务命令的使用——面向任务设计](example_task-oriented_command_zh.md)

### 第一步：注册线程池
```java
public static final String MY_POOL = "my_pool";
static {
    Terse.registerExecutor(MY_POOL, Executors.newFixedThreadPool(2));
}
```

### 第二步： 定义context
```java
@Getter
@Setter
class FlowContext implements IFlowContext {
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
}
```

### 第三步： 实现ITask接口定义流程任务

实现ITask接口需要实现其4个任务和条件依赖方法，但为了文档篇幅尽量少点，我们先定义一个叫SimpleReliable的接口默认实现这些方法，代码如下
```java
public interface SimpleReliable<C extends IFlowContext> extends IReliableTask<C> {

   @Override
   default List<ITask<C>> mustRelyOnTasks() {return null;}
   @Override
   default List<ITask<C>> randomRelyOnTasks() {return null;}
   
   @Override
   default List<IConditionTask<C>> mustConditions(){return null;}
   @Override
   default List<IConditionTask<C>> randomConditions() {return null;}

}
```
下面我们来实现h1、h2、h3、h4、h5这5个任务的代码。

- 定义任务h1=A+B
```java
class H1 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
   static H1 INSTANCE = new H1();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
           return context.getA() + context.getB();
        }, v -> {
            context.setH1(v);
        });
    }
}
```

- 定义任务h2=C+D
```java
class H2 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
   static H2 INSTANCE = new H2();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getC() + context.getD();
        }, v -> {
            context.setH2(v);
        });
    }
}
```

- 定义任务h3=E-F
```java
class H3 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
   static H3 INSTANCE = new H3();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getE() - context.getF();
        }, v -> {
            context.setH3(v);
        });
    }
}
```

- 定义任务h4=h1*h2
   
   h4任务需要依赖H1和H2任务执行完后才能执行，代码如下：
```java
class H4 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H4 INSTANCE = new H4();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return Arrays.asList(H1.INSTANCE, H2.INSTANCE);
    }
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getH1() * context.getH2();
        }, v -> {
            context.setH4(v);
        });
    }
}
```

- 定义任务h5=h4/h3

   h5任务需要依赖H3和H4任务执行完后才能执行，代码如下：
```java
class H5 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H5 INSTANCE = new H5();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return Arrays.asList(H3.INSTANCE, H4.INSTANCE);
    }
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getH4() / context.getH3();
        }, v -> {
            context.setH5(v);
        });
    }
}
```


### 第四步：定义summary任务打印结果
```java
class Summary implements ISummaryTask<FlowContext>, SimpleReliable<FlowContext> {
   @Override
   public List<ITask<FlowContext>> mustRelyOnTasks() {
      return Collections.singletonList(H5.INSTANCE);
   }
   @Override
   public void summary(FlowContext context) {
      // summary result
      System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
              context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
   }
}
```

### 第五步：执行任务
```java
// generate context and params data
FlowContext context = new FlowContext(summary);
context.setA(1);context.setB(2);
context.setC(3);context.setD(4);
context.setE(5);context.setF(6);

// execute flow
Terse.execute(new Summary(), context);
```

**执行结果**
```text
(1+2)*(3+4)/(5-6) = -21
```



### 总结
在上面的示例中不难看出，
1. 每个任务只关注自己依赖的任务，而不用关注整个流程需要哪些任务。
2. 流程需要一个ISummaryTask对象来统管其任务，这个summary不需要关注流程的所有任务，只需要关注最后一批执行的任务即可。