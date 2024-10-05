# 案例1：异步任务命令的使用——面向流程设计

下面演示的是在面向流程设计中如何使用[**异步任务命令**] 来执行任务。

## 案例
以算术表达: (A+B)*(C+D)/(E-F)为例，DAG流程图如下，为了简单表达，我们的数字都是整型。

[//]: # (![flow1.jpg]&#40;https://s21.ax1x.com/2024/05/19/pku7iE4.jpg&#41;)
![flow1.jpg](./image/flow1.jpg)

### 实现代码

1. 定义操作符枚举
```java
public enum Op {
    ADD, SUBTRACT, MULTIPLY, DIVIDE;
}
```

2. 定义一个计算命令类
```java
@Getter
public class CalculateCommand implements ICommandX<Integer> {
    private int id;
    private final Op op;
    private final int x;
    private final int y;
    public CalculateCommand(int x, Op op, int y) {
        this.x = x;
        this.op = op;
        this.y = y;
    }
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}
```

3. 定义命令接收者工厂
```java
public class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand> {
    @Override
    public void buildReceiver(IReceiverBuilder<CalculateCommand> builder) {
        builder.onReceive(CalculateCommand.class, command -> {
                    switch (command.getOp()) {
                        case ADD:
                            return command.getX() + command.getY();
                        case SUBTRACT:
                            return command.getX() - command.getY();
                        case MULTIPLY:
                            return command.getX() * command.getY();
                        case DIVIDE:
                            return command.getX() / command.getY();
                        default:
                            throw new IllegalArgumentException("不支持的操作");
                    }
                })
                // 这里设置线程池执行来执行上面命令
                .defaultExecutor(ForkJoinPool.commonPool());
    }
}
```

4. 定义流程上下文
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

    @Override
    public long timeout() {
        return 0;
    }

}
```

5. 定义流程，下面是基于流的定义，可以定义为一个全局变量
```java
static TerseFlow<FlowContext> flow = Terse.<FlowContext>flow()
    // h1
    .runCommand(context -> new CalculateCommand(context.getA(), Op.ADD, context.getB())
            , (context, v) -> context.setH1(v))
    // h2
    .runCommand(context -> new CalculateCommand(context.getC(), Op.ADD, context.getD())
            , (context, v) -> context.setH2(v))
    .then() // then go h4 task
    // h4
    .runCommand(context -> new CalculateCommand(context.getH1(), Op.MULTIPLY, context.getH2())
            , (context, v) -> context.setH4(v))
    .then() // then go h5 task
    // rely on h3 task
    .mustRelyOn(Terse.<FlowContext>flow().runCommand(context -> new CalculateCommand(context.getE(), Op.SUBTRACT, context.getF())
            , (context, v) -> context.setH3(v)))
    // h5
    .runCommand((context) -> new CalculateCommand(context.getH4(), Op.DIVIDE, context.getH3())
            , (context, v) -> context.setH5(v))
    // build flow
    .build(context -> {
        // summary result
        System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
                context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
    });
```

6. 注册命令接收者，注册接收者只需执行一次就可以了，方式有多种，下面是其中一种通过java方法直接注册。
```java
// 注册命令接收者
Terse.registerReceiverFactory(new CalculateReceiverFactory());
```

7. 执行流程, 示例如下
```java
// generate context and params data
FlowContext context = new FlowContext();
context.setA(1);
context.setB(2);
context.setC(3);
context.setD(4);
context.setE(5);
context.setF(6);

// 执行流程并等待结果
flow.execute(context);
System.out.printf("执行结果：%s \n", context.getH5());
```