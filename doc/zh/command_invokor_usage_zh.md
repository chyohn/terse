# 在流程外提交异步任务

命令对象的应用有两个场景，本篇介绍为场景二。
- 场景一：在流程中创建命令，而由任务调度器自动提交给Receiver。
- 场景二：在流程外提交异步命令，直接创建命令，并通过ICommandInvoker接口提供的方法提交给Receiver执行

ICommandInvoker接口定义了几个提交命令的便捷方法，
包括同步执行命令、异步执行命令，也可以提交Runnable和Callable这种封装线程任务的对象。

开始前，先说下ICommandInvoker对象的获取，代码如下
```java
ICommandInvoker executor = Terse.commandInvokor();
```

## ICommandInvokor的方法

### 1. 同步提交命令

方法名 | 描述
--- | ---
run(ICommandX<R> command) | 同步执行命令，并返回命令结果
run(List<ICommandX<R>> commands, Consumer<IResult<R>> callback) | 同时提交多条命令，同步等待结果，只要收到一条结果就执行回调callback，直到所有结果处理完成才从方法返回。

示例1： 同步执行单条命令
```java
// 执行一个加法命令
Integer result = Terse.commandInvokor().run(new AddCommand(1, 2));
System.out.println("result: " + result);
```
示例2： 同步执行多条命令

```java
List<AddCommand> commands = new ArrayList<>();
commands.add(new AddCommand(1, 2));
commands.add(new AddCommand(5, 3));
Terse.commandInvokor().run(commands, result -> {
    AddCommand command = commands.get(result.getId());
    System.out.printf("%s + %s = %s", command.getX(), command.getY(), result.getValue());
});
```

### 2. 异步提交命令
方法名 | 描述
--- | ---
asyncRun(ICommandX<R> command, Consumer<R> callback) | 异步提交一条命令，命令执行完在异步线程中执行回调方法
asyncRun(List<ICommandX<R>> commands, Consumer<IResult<R>> callback)| 异步提交多个命令，每执行完一个命令就在异步线程中执行回调

示例3： 同步执行单条命令
```java
// 执行一个加法命令
Terse.commandInvokor().asyncRun(new AddCommand(1, 2), result -> {
    System.out.println(result);
});
```

示例4： 同步执行多条命令
```java
List<AddCommand> commands = new ArrayList<>();
commands.add(new AddCommand(1, 2));
commands.add(new AddCommand(5, 3));
Terse.commandInvokor().asyncRun(commands, result -> {
    AddCommand command = commands.get(result.getId());
    System.out.printf("%s + %s = %s", command.getX(), command.getY(), result.getValue());
});
```

### 3. 提交Runnable和Callable对象给指定线程池

方法名 | 描述
--- | ---
asyncRun(String poolName, Runnable runnable) | 提交runnable对象到指定线程池，如果poolName指定的线程池不存在，则使用公共线程池
asyncRun(String poolName, Callable<R> callable, Consumer<R> callback) | 提交callable对象到指定线程池，如果poolName指定的线程池不存在，则使用公共线程池

示例5： 提交Runnable任务
```java
Terse.commandInvokor().asyncRun("my_pool", () -> {
    System.out.println("Runnable thread name: " + Thread.currentThread().getName());
});
```

示例6： 提交Callable任务
```java
Terse.commandInvokor().asyncRun("my_pool", () -> {
    System.out.println("Callable thread name: " + Thread.currentThread().getName());
    return true;
}, result -> {
    System.out.printf("callback result: %s,  thread name: %s \n", result, Thread.currentThread().getName());
});
```