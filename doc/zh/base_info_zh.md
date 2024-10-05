# 基本概念

## 1. 概述
## 1.1 Terse流程任务的特征
1. 协程特性，流程中所有任务命令创建和任务结果回调处理都在同一个线程中执行。
2. 流程中任务命令被默认线程池或指定的线程池异步处理。而命令的处理结果交由任务命令创建线程处理（第1点）。
3. 各个任务的定义基于先行发生原则，即写任务先于读任务执行。

这3个特征保障了我们可以线程安全的操作流程的Context对象。

## 1.2 流程的定义方式
1. **面向流程设计：** 使用Terse.flow()方法开启流式编程风格进行代码编写。优点是可以直观的表达流的过程，适用于比较简单的节点数不多的流程。
2. **面向任务设计：** 可以轻松应对复杂流程的定义，该方式为分治思想，即把流程定义的关注点转移到各个任务上，通过任务的定义来完成流程的定义。
   实现方式：
    - 每个任务节点实现ITask接口，在每个实现中只关注自己依赖的Task
    - 实现ISummaryTask接口来表示一个流的end节点，在这个实现把流程最后一批执行的节点作为依赖。

## 1.3 任务的定义方式
1. **线程池任务：** 把任务执行逻辑通过Runnable或Callable对象包装，提交到指定的线程池中执行。任务定义代码如下：
```java
class ExampleTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        // 定义任务，创建ITaskHandler，在主线程中执行
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            // 线程池任务，在MY_POOL指定的线程中执行
           return context.getSomeValue1() + context.getSomeValue2();
        }, v -> {
            // 结果回调，在主线程中执行
            context.setSomeResult(v);
        });
    }
    // other methods...
}
```
2. **异步命令任务：** 把任务执行逻辑封装在特定Receiver对象中，流程执行时通过提交命令来异步执行Receiver中任务逻辑。任务定义代码如下：
```java
class ExampleTask implements ITask<Context> {
   @Override
   public ITaskHandler createTaskHandler(Context c) {
      // 定义任务，创建ITaskHandler，在主线程中执行
      return TaskHandlerFactory.newTaskHandler(() -> {
          // 创建一个加法命令，在主线程中执行
         return new AddCommand(c.getSomeValue1(),c.getSomeValue2());
      }, v -> {
         // 结果回调，在主线程中执行
         c.setSomeResult(v);
      });
   }
   // other methods...
}
```
示例中创建并提交了一个AddCommand命令，该命令类定义和命令的处理逻辑需要单独定义，如何定义可见[创建异步任务命令处理器](command_receiver_usage_zh.md)

## 1.4 任务的依赖关系
1. **强依赖：** 比如任务B强依赖任务A，语义如下：
   - 只有任务A执行完成，任务B才可以执行。
   - 如果任务A因为某些条件不满足而不能执行，则任务B也不能执行。
2. **弱依赖：** 又叫随机依赖，比如任务C弱依赖于任务A和B，语义如下：
   - 只有任务A和B中任意一个执行完成，就可以执行任务C。
   - 如果任务A和任务B因为某些条件不满足而不能执行，则任务C也不能执行。
3. **必要条件依赖：** 当前任务只有所有必要条件都满足才能执行。
   - 多个必要条件的关系为“&&”关系，即都必须要满足。
4. **任意条件依赖：** 当前任务只要有任意一个条件满足就可以执行。
   - 多个任意条件的关系为“||”关系，即只要一个满足就行。
   - 任意条件列表与必要条件列表是and关系，即：(必1 && 必2 &&..) && (任1 || 任2 ||...)


## 2. 流程的主要角色和概念

### 2.1 流程数据中转站——IFlowContext
所有context需要实现接口IFlowContext来存放任务结果。使用IFlowContext接口约束的目的
- 利于后期扩展兼容
- 任务节点产生的结果数据使用一个独立字段存放，可读性高些。

示例如下：
```java
public class MyContext implements IFlowContext {
//...
}
```

### 2.2 任务定义——ITask和ITaskHandler
1. 每个任务节点都会实现ITask接口来定义任务内容和任务依赖的资源（来自其他任务产生）。
2. 任务的内容包括构建异步任务命令和命令结果处理，这两件事情其实是委托给ITaskHandler接口实现的。
3. 在实际编码中通过TaskHandlerFactory提供的 工厂方法创建ITaskHandler对象。
4. 如果代码并不复杂或者命令的构建与结果的处理没有关联关系，推荐使用ITask的两个扩展类ITaskSingle和ITaskMultiply。

示例如下：
```java
class ExampleTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        // 定义任务，创建ITaskHandler，在主线程中执行
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            // 任务提交后，在MY_POOL指定的线程中执行
           return context.getSomeValue1() + context.getSomeValue2();
        }, v -> {
            // 结果回调，在主线程中执行
            context.setSomeResult(v);
        });
    }
    // other methods...
}
```

### 2.3 任务结果汇总——ISummaryTask
1. 每个流程都有一个summary节点作为结束
2. summary中流程最后的任务节点作为依赖任务
3. 在summary中可以做一些汇总操作，summary中的所有方法都在主线程中执行

### 2.4 流程执行器——IFlowExecutor
作为流程执行的入口，推荐使用Terse.execute(summary, context)方法启动流程执行任务。

## 3. 流程执行原理

![flow_sequence.jpg](https://img-blog.csdnimg.cn/direct/ad87e1c6eab94861859cca9b38d25c48.jpeg)

对于flow执行线程(调Terse.execute()方法的线程）：
1. flow提交了3个命令，可能是一个任务节点提交的也可能是3个任务节点提交的，这个不重要。
2. 命令提交后，如果没有其它命令可提交则poll结果队列，等待数据返回 只要有结果返回就处理结果，
3. 处理完返回的结果后，如果这个结果对应的任务节点所有的命令都执行完成，则检查这个任务节点后续的任务节点所依赖的任务是否都完成，
4. 如果后续节点依赖的任务节点都完成了则回到第1步，把所有可执行的后续节点的命令提交给Receiver
5. 只要有命令没有返回结果，就一致poll结果队列，直到所有任务节点的命令都执行完毕。

对于Receiver执行线程：
1. Receiver收到命令，如果命令注册处理器是异步方法，则直接执行这个异步方法，结果由异步方法决定返回时机。
2. 如果命令注册处理器是同步方法，则使用注册的线程池，如果没有注册处理命令的线程池，则使用默认线程池，执行命令的处理器方法。
3. 同步方法返回结果，或异步方法回调结果，都会把结果put到结果队列。
