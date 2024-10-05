# 快速开始——面向流程设计

## 一、面向流程设计介绍
面向流程设计使用Terse.flow()方法开启流式编程风格进行代码编写。优点是可以直观的表达流的过程，适用于比较简单的节点数不多的流程。

Terse.flow()是在面向任务设计接口的基础上实现的一套流式编程风格的接口。要了解面向任务设计见[1.3 快速开始——面向任务设计](doc/zh/quickstart_task-oriented_zh.md)


### 1. 入口方法
1. 使用Terse.flow()创建一个TerseFlowBuilder，这个builder提供几个方法用于构建流的执行节点、跳转后续节点和处理节点依赖、合并其他流程。
```java
TerseFlowBuilder<SomeContext> builder = Terse.<SomeContext>flow();
```
2. 使用Terse.<Context>condition()创建一个TerseConditionBuilder，这个builder用于创建判断条件，以及添加条件所依赖的任务或其他条件。
```java
TerseConditionBuilder<SomeContext> builder = Terse.<SomeContext>condition(context->{条件表达式});
```

### 2. TerseFlowBuilder提供的方法
该对象用于创建流程，并提供几个方法用于创建任务节点、跳转后续节点、依赖其它节点、合并其他流程。下面一一介绍。

#### 2.1 用于构建任务的方法

方法名| 说明 
-|-
runnable | 表示指定一个线程池来执行节点任务，没有返回值
callable | 和runnable一样，不过有返回值
runCommand | 表示创建一个发起一条执行命令的节点
runCommands | 创建一个可以发起多条执行命令的节点

这些方法可以多次定义，表示多个相互之间没有依赖的节点可以并行执行，比如下面定义了3个并行任务
```java
builder.runnable("pool1", context -> doTask1())
        .runnable("pool1", context -> doTask2())
        .callable("poo1", context -> doTask3AndGetValue(), (context, value)->context.setValue(value))
```

#### 2.2 跳转后续任务

方法名| 说明 
-|-
then | 表示当前所有任务执行完可以执行后续的任务，返回新的TerseFlowBuilder对象。

案例：任务1和任务2执行完后才能执行任务3，
```java
builder.runnable("poo1", context -> doTask1())
        .runnable("pool1", context -> doTask2())
        .then()
        .callable("poo1", context -> doTask3AndGetValue(), (context, value)->context.setValue(value))
```

#### 2.3 依赖其它任务
方法名| 说明
-|-
mustRelyOn | 添加强依赖任务，表示当前所有任务都依赖某个流程或节点。换句话的意思是指定依赖流程执行完后才可以执行当前所有节点。
randomRelyOn | 添加弱依赖任务，表示在某些流程或节点中，至少一个完成就可以执行当前所有节点。

如果既有强依赖任务，又有弱依赖任务时，当前任务执行的条件是：所有强依赖任务执行完成，且任意一个弱依赖任务执行完成。

比如下面：任务1和任务2执行完后才能执行任务3
写法一:
```java
TerseFlowBuilder<SomeContext> task1 = Terse.<SomeContext>flow().runnable("poo1", context -> doTask1());
TerseFlowBuilder<SomeContext> task2 = Terse.<SomeContext>flow().runnable("pool1", context -> doTask2());
builder.mustRelyOn(task1)
        .mustRelyOn(task2)
        .callable("poo1", context -> doTask3AndGetValue(), (context, value)->context.setValue(value))
```
写法二：混用then()和relyOn()
```java
TerseFlowBuilder<SomeContext>  task1 = Terse.<SomeContext>flow().runnable("poo1", context -> doTask1());
builder.runnable("pool1", context -> doTask2())
        .then()
        .mustRelyOn(task2)
        .callable("poo1", context -> doTask3AndGetValue(), (context, value)->context.setValue(value))
```

#### 2.4 依赖某个条件
方法名| 说明
-|-
mustCondition | 必要条件，表示当前所有任务只有在满足某一条件时才能执行，如果条件不满足当前所有任务不会执行。
randomCondition | 任意满足一个条件，表示在某些流程或节点中，至少有一个条件满足就可以执行当前所有任务。

如果既有必要条件，又有任意条件时，当前任务执行的条件是：所有必要条件必须满足，且任意条件中至少一个条件满足。

例1：节点任务value=A/B，必须B!=0;
```java
Terse.<SomeContext>flow()
        .mustCondition(Terse.<SomeContext>condition(c -> c.getB() != 0))
        .callable("poo1", c -> c.getA()/c.getB(), (context, value)->context.setValue(value))
```
例2：节点任务h1=A/(B+C)，必须B!=0或者C!=0;
```java
Terse.<SomeContext>flow()
        .randomCondition(Terse.<SomeContext>condition(c -> c.getB() != 0))
        .randomCondition(Terse.<SomeContext>condition(c -> c.getC() != 0))
        .callable("poo1", c -> c.getA()/(c.getB()+c.getC()), (context, value)->context.setValue(value))
```

#### 2.5 合并其它流程

方法名| 说明 
-|-
merge | 合并两个流，两个流属于并列关系，可以并行执行。



比如流程A： a1 -> a2  ;流程B：b1 -> b2。代码如下
```java
TerseFlowBuilder<SomeContext> flowA = Terse.<SomeContext>flow()
    .runnable("pool1",context->a1())
    .then()
    .runnable("pool1", context->a2());
    
TerseFlowBuilder<SomeContext> flowB = Terse.<SomeContext>flow()
    .runnable("pool1",context->b1())
    .then()
    .runnable("pool1", context->b2());
    
TerseFlowBuilder<SomeContext> builder = Terse.<SomeContext>flow().merge(flowA).merge(flowB);
```

merge方法和relyOn方法的区别
- relyOn加入的流程与当前节点属于依赖关系，所加入的流程执行完才能执行当前的节点。
- merge加入的流程与当前节点属于并列关系，它们可以并行执行。


#### 2.6 构建流程

方法名| 说明 
-|-
build | 创建TerseFlow对象
```java
TerseFlow<SomeContext> flow = builder.build();
```

构建一个IFlowContext对象参数，就可以使用TerseFlow对象的execute方法执行流程

### 3. TerseConditionBuilder提供的方法
TerseConditionBuilder用于创建判断条件，以及添加条件所依赖的任务或其他条件。该对象通过下面方式获得
```java
TerseConditionBuilder<SomeContext> builder = Terse.<SomeContext>condition(context->{条件表达式});
```

#### 3.1 依赖其他任务
方法名| 说明
-|-
mustRelyOn | 添加强依赖任务，表示当前条件节点依赖某个流程或节点。换句话的意思是指定依赖流程执行完后才可以执行当前条件节点。
randomRelyOn | 添加弱依赖任务，表示在某些流程或节点中，至少一个完成就可以执行条件节点。

如果既有强依赖任务，又有弱依赖任务时，当前条件执行的前提是：所有强依赖任务执行完成，且任意一个弱依赖任务执行完成。

例1：比如条件1需要依赖任务1执行获得结果才能执行
```java
TerseFlowBuilder<SomeContext> task1 = Terse.<SomeContext>flow().runnable("poo1", context -> doTask1());
TerseConditionBuilder<SomeContext> condition1 = Terse.<SomeContext>condition(c -> c.getTask1Result() != 0)
        .mustRelyOn(task1);
```

#### 3.2 依赖其他某个条件
方法名| 说明
-|-
mustCondition | 必要条件，表示当前条件节点只有在满足某一条件时才能执行，如果条件不满足当前节点不会执行。
randomCondition | 任意满足一个条件，表示至少有一个条件满足就可以执行当前节点。

如果既有必要条件，又有任意条件时，当前条件节点执行的前提是：所有必要条件必须满足，且任意条件中至少一个条件满足。

例1：条件2依赖条件1，只有条件1满足时才执行条件2。
```java
TerseConditionBuilder<SomeContext> condition1 = Terse.<SomeContext>condition(c -> c.getOtherResult() != 0);
TerseConditionBuilder<SomeContext> condition2 =Terse.<SomeContext>condition(c -> c.getSomeResult() != 0)
        .mustCondition(condition1);
```

#### 3.3 构建流程

方法名| 说明 
-|-
build | 创建IConditionTask对象，无需显示执行，Terse Flow会在需要的地方执行。


### 4 完整定义过程

```java
// 1. 定义context
class SomeContext implements IFlowContext {...}
// 2. 定义流程
TerseFlow<SomeContext> flow = Terse.<SomeContext>flow().runnable(...).build();
// 3. 构建context参数
SomeContext context = ...;
// 4. 执行流程
flow.execute(context);
// 5. 获取结果
System.out.println(context.getSomeResult())

// 如果基于命令模式异步执行任务，还要完成后面3步
// 6. 实现ICommandx<ReturnType>
class MyCommand implements ICommandX<MyReturnType> {
// ...
}
// 7. 实现IReceiverFactory<MyCommand>
class MyReceiverFactory implements IReceiverFactory<MyCommand> {
// ...
}
// 8. 注册ReceiverFactory，有两种方式，这里使用其中一种作为示例
Terse.registerReceiverFactory(new MyReceiverFactory());
```

## 二、示例

以算术表达式 (A+B)*(C+D)/(E-F)为例，DAG图如下

[//]: # (![flow1.jpg]&#40;https://s21.ax1x.com/2024/05/19/pku7iE4.jpg&#41;)
![flow1.jpg](./image/flow1.jpg)

本示例中以创建[**线程池任务**]为例做演示，若要看[**异步任务命令**]的演示见
[2.1 案例1：异步任务命令的使用——面向流程设计](example_flow-oriented_command_zh.md)

### 第一步：注册线程池

```java
public static final String MY_POOL = "my_pool";
static {
    Terse.registerExecutor(MY_POOL, Executors.newFixedThreadPool(2));
}
```

### 第二步：定义Context
```java
// Implement Context
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

### 第三步：定义流程
使用第一步中注册的线程池执行每个任务
```java
// define flow task and execute
TerseFlow<FlowContext> flow = Terse.<FlowContext>flow()
        // h1
        .callable(MY_POOL, context -> context.getA() + context.getB(), (context, v) -> context.setH1(v))
        // h2
        .callable(MY_POOL, context -> context.getC() + context.getD(), (context, v) -> context.setH2(v))
        .then() // then go h4 task
        // h4
        .callable(MY_POOL, context -> context.getH1() * context.getH2(), (context, v) -> context.setH4(v))
        .then() // then go h5 task
        // rely On h3 task
        .mustRelyOn(Terse.<FlowContext>flow().callable(MY_POOL, context -> context.getE() - context.getF(), (context, v) -> context.setH3(v)))
        // h5
        .callable(MY_POOL, (context) -> context.getH4() / context.getH3(), (context, v) -> context.setH5(v))
        // build flow
        .build(context -> {
            // summary result
            System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
                    context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
        });
```

### 第四步：创建参数执行流程
```java
// generate context and params data
FlowContext context = new FlowContext(summary);
context.setA(1);context.setB(2);
context.setC(3);context.setD(4);
context.setE(5);context.setF(6);

// execute flow
flow.execute(context);
```

#### 执行结果
```text
(1+2)*(3+4)/(5-6) = -21
```
