# 创建异步任务命令处理器

Terse提交异步任务是基于命令模式实现，使用此方式可以进一步提高代码复用，比如发起http请求：可以把参数封装到命令对象中，而发起http请求的逻辑交由Receiver来处理，
这样上层代码只需要根据业务需求组装命令，而无需再编写如何发起http请求。

下表是Terse实现与命令模式参照表

命令模式元素 | Terse实现                                      | 说明
-----------|----------------------------------------------|---------
命令对象    | ICommand、ICommandX                           | ICommandX继承ICommand, 建议使用ICommandX，可以明确指定命令返回只类型
命令执行者  | ICommandInvoker                              | 流程任务中无需显示使用，如需在流程外使用它提交任务，可通过Terse.commandInvoker()接口获取其实例
命令接收者  | IReceiverFactory、IReceiver、IReceiverBuilder  | 业务中是需要通过实现IReceiverFactory接口来定义接收者如何处理命令（异步任务）


## IReceiverFactory接口

接口定义如下，它用于管理同类型（由下面泛型参数T指定）的命令对应的处理器。
结合IReceiverBuilder接口提供的一些便捷方法，可以更方便的注册命令处理器。
```java
public interface IReceiverFactory<T extends ICommand> {
    /**
     * build receiver
     *
     * @param builder
     */
    void buildReceiver(IReceiverBuilder<T> builder);
}
```
### 注册IReceiverFactory实现
实现IReceiverFactory<Command>接口后需要注册后才能被使用到，注册方式有3种方式
1. 方式一：手动调Terse提供的静态方法注册接口
```java
Terse.registerReceiverFactory(new MyReceiverFactory1());
Terse.registerReceiverFactory(new MyReceiverFactory2());
Terse.registerReceiverFactory(new MyReceiverFactory3());
```
2. 方式二：通过在classpath的META-INF目录下添加parallel.factories文件，文件内容示例如下
```text
io.github.chyohn.terse.command.IReceiverFactory = 我的包路径1.MyReceiverFactory1 \
    我的包路径2.MyReceiverFactory2 \
    我的包路径3.MyReceiverFactory3 \
```
3. 方式三：（推荐）通过实现IReceiverFactoryLoader接口进行加载，另外这个实现类还是需要采用方式二parallel.factories文件中注册IReceiverFactoryLoader接口，注册示例如下
```text
io.github.chyohn.terse.command.IReceiverFactoryLoader = 我的包路径1.MyReceiverFactoryLoader
```


## IReceiverBuilder

接口方法列表如下

序号| 方法名 |描述
---|--------------------------------------------------------------------------------------|---
1  | defaultExecutor(Executor executor)                                                   | 设置默认的异步处理线程池，当没有找到命令的异步处理线程池时，会使用该线程池处理                                           
2  | executor(Class<T> t, ExecutorFactory<T> executorFactory)                             | 设置指定命令类型的异步处理线程池，当收到该类型的命令时，会使用该线程池处理                                             
3  | onReceive(Class<T> t, BlockingMethod<T> handler)                                     | 命令处理器handler。在Receiver对象收到命令并执行该hander时会等待handler执行完，最后把结果返回给命令发送者                
4  | onReceive(Class<T> t, BlockingMethod<T> handler, ExecutorFactory<T> executorFactory) | 注册命令处理器和其对应的异步线程池，是上面第2和第3两个方法的组合使用                                                    
5  | onReceive(Class<T> t, AsyncMethod<T> handler)                                        | 命令异步处理器handler，用于已有异步方法的情况，如非阻塞IO请求、异步IO请求。在Receiver对象收到命令并执行该hander时不会等待handler执行完，而结果交由handler决定何时返回给命令发送者。    
6  | build()                                                                              | 完成注册，返回IReceiver对象。                                   

说明：
1. 方法3和4用于只有同步方法的情况，如同步IO请求、内存耗时任务。
2. 方法5用于已有异步方法的情况，如非阻塞IO请求、异步IO请求。
3. 第6个方法build()，无需显示调用，Terse会在合适的时候自动执行。

## 示例

### 第一步：抽象命令
我们仍然以算术计算器为例。为了更好的展示IReceiverBuilder的使用，把算术命令具体化为加法、减法、乘法、除法四种命令。首先定义一个抽象类CalculateCommand类，示例代码如下。
```java
@Getter
public abstract class CalculateCommand implements ICommandX<Integer> {
    private int id;
    private final int x;
    private final int y;
    public CalculateCommand(int x, int y) {
        this.x = x;
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

### 第二步：实现四种具体的算术命令类

示例代码如下。
```java
// 加法命令
static class AddCommand extends CalculateCommand {
    public AddCommand(int x, int y) {
        super(x, y);
    }
}

// 减法命令
static class SubCommand extends CalculateCommand {
    public SubCommand(int x, int y) {
        super(x, y);
    }
}

// 乘法命令
static class MulCommand extends CalculateCommand {
    public MulCommand(int x, int y) {
        super(x, y);
    }
}

// 除法命令
static class DivCommand extends CalculateCommand {
    public DivCommand(int x, int y) {
        super(x, y);
    }
}
```

### 第三步：实现命令处理器
```java
class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand> {
    // 默认命令线程池
    final static Executor DEFAULT_EXECUTOR = Executors.newFixedThreadPool(2);
    // 加法命令线程池
    final static Executor ADD_EXECUTOR = Executors.newFixedThreadPool(2); 
    // 乘法命令线程池
    final static Executor MUL_EXECUTOR = Executors.newFixedThreadPool(2);
    // 除法命令线程池
    final static Executor DIV_EXECUTOR = Executors.newFixedThreadPool(2);

    @Override
    public void buildReceiver(IReceiverBuilder<CalculateCommand> builder) {
        builder
            .defaultExecutor(DEFAULT_EXECUTOR)
            .executor(AddCommand.class, command -> { // 为加法命令指定线程池
                return ADD_EXECUTOR;
            })
            .onReceive(AddCommand.class, command -> { // 注册加法命令处理器
                return add(command);
            })
            .onReceive(SubCommand.class, command -> { // 注册减法命令处理器
                return command.getX() - command.getY();
            })
            .onReceive(MulCommand.class, command -> { // 注册乘法命令处理器
                return mul(command);
            }, command -> { // 为乘法命令指定线程池
                return MUL_EXECUTOR;
            })
            .onReceive(DivCommand.class, (command, callback) -> { // 注册除法命令处理器，直接使用除法的异步方法
                asyncDivCommand(command, result -> {
                    callback.accept(result);
                });
            });
    }

    Integer add(AddCommand command) {
        return command.getX() + command.getY();
    }

    Integer mul(MulCommand command) {
        return command.getX() * command.getY();
    }
    
    // 异步方法处理除法命令
    void asyncDivCommand(DivCommand command, Consumer<Object> callback) {
        CompletableFuture.runAsync(() -> {
            callback.accept(command.getX() / command.getY());
        }, DIV_EXECUTOR);
    }

}
```
示例说明：
1. IReceiverFactory的泛型参数类型指定为抽象类CalculateCommand，表示该工厂管理的是CalculateCommand类型的命令，我们就可以在该工厂中定义其所有子类的命令处理器。
2. 加、减、乘三个命令的处理器都是同步的，直接返回计算结果即可。
3. 除法命令是异步的，使用CompletableFuture.runAsync方法异步处理，处理完后通过Consumer回调返回结果。
4. 线程池方面：
    - 默认线程池DEFAULT_EXECUTOR用于Receiver异步处理所有未指定线程池的命令，比如减法命令；
    - ADD_EXECUTOR用于Receiver异步处理加法命令；
    - MUL_EXECUTOR用于Receiver异步处理乘法命令；
    - DIV_EXECUTOR用于在异步方法中处理除法命令。

### 第四步：注册IReceiverFactory实现类

CalculateReceiverFactory只有注册到Terse中才能生效，普通的注册方式有3种，下面示例我们使用第一种最直接的方式。
（如果使用spring，还可以有其它可操作性更好的方式，见[与Spring和Spring boot集成](spring_support_zh.md)）
```java
public class Main {
    public static void main(String[] args) {
        // 注册CalculateReceiverFactory
        Terse.registerReceiverFactory(new CalculateReceiverFactory());
        
        // 执行一个加法命令
       Integer result = Terse.commandExecutor().run(new AddCommand(1, 2));
       System.out.println("result: " + result);
    }
}
```


## 总结

IReceiverFactory、IReceiver、IReceiverBuilder三个的关系，就之于ITask、ITaskHandler、TaskHandlerFactory三个的关系（见[构建任务——ITask和TaskHandlerFactory的使用](task_usage_zh.md)）。
我们不需要显示与IReceiver和ITaskHandler打交道，只需要实现IReceiverFactory和ITask接口，然后通过IReceiverBuilder注册命令处理器，TaskHandlerFactory创建ITaskHandler。
这样，我们就可以很方便的实现命令和任务的异步处理。
