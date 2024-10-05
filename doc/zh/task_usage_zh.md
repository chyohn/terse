# 构建任务——ITask和TaskHandlerFactory的使用

## 1. ITask接口
ITask用于构建任务，接口定义如下：
```java
public interface ITask<C extends IFlowContext> extends IReliableTask<C> {

    /**
     * provide the task handler who provide command and result handler
     *
     * @param context flow context
     * @return can return null if no async command to do.
     */
    ITaskHandler createTaskHandler(C context);
}
```
ITask接口提供如下两个扩展接口

接口名| 说明 
-|-
ITaskSingle | 扩展ITask接口，表示任务只发起一个命令
ITaskMutiply | 扩展ITask接口，表示任务发起多个命令


## 2. TaskHandlerFactory和它的工厂方法
TaskHandlerFactory用于辅助ITask实现类在createTaskHandler方法中创建ITaskHandler对象。提供了如下工厂方法

方法名|描述
---|---
newTaskHandler(String poolName, Runnable runnable)|在指定线程池中执行runnable任务
newTaskHandler(String poolName, List<Runnable> runnableList)| 一次性提交多个runnable任务给指定线程池
newTaskHandler(String poolName, Callable<R> callable, Consumer<R> responseHandler) | 在指定线程池中执行callable任务，并在任务完成后调用responseHandler处理返回结果
newTaskHandler(String poolName, List<Callable<R>> callables, Consumer<R> responseHandler) | 一次性提交多个callable任务给指定线程池，并在所有任务完成后调用responseHandler处理返回结果
newTaskHandler(Supplier<ICommandX<R>> commandXSupplier, Consumer<R> responseHandler) | 提交一个异步命令，并在任务完成后调用responseHandler处理返回结果
newTaskHandler(Supplier<List<ICommandX<R>>> commandXSupplier, Callback2<IResult<R>, Boolean> responseHandler)| 提交多个异步命令，并在所有任务完成后调用responseHandler处理返回结果

说明：往线程池中提交任务时，如果线程池不存在，则会使用公共线程池（ForkJoinPool.commonPool）执行任务。

下面给出几个使用示例：

## 3. 指定线程池执行任务

### 3.1 注册线程池，全局只需要注册一次
```java
// 注册线程池，全局只需注册一次
static String MY_POOL = "myPool";
static {
    Terse.registerExecutor(MY_POOL, Executors.newFixedThreadPool(2));
}
```

### 3.2 使用线程池执行Runnable任务
方式1： 使用TaskHandlerFactory创建ITaskHandler对象
```java
class RunnableTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        ITaskHandler handler = TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            System.out.println("runnable");
        });
        return handler;
    }
    // other methods ...
}
```

方式2： 使用ITaskSingle<ContextType, CommandReturnType>扩展接口
```java
class RunnableTask implements ITaskSingle<MyContext, Boolean> {
    @Override
    public ICommandX<Boolean> getCommand(MyContext context) {
        return new RunnableCommand(MY_POOL, () -> {
            System.out.println("runnable");
        });
    }
    @Override
    public void handleResult(MyContext context, Boolean result) {

    }
    // other methods ...
}
```

### 3.3 使用线程池执行Callable任务
```java
class CallableTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        ITaskHandler handler = TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return "callable";
        }, v -> {
            System.out.println(v);
        });
        return handler;
    }
    // other methods ...
}
```

方式2： 使用ITaskSingle<ContextType, CommandReturnType>扩展接口
```java
class CallableTask implements ITaskSingle<MyContext, String> {
    @Override
    public ICommandX<String> getCommand(MyContext context) {
        return new CallableCommand(MY_POOL, () -> {
            return "callable";
        });
    }
    @Override
    public void handleResult(MyContext context, String result) {
        System.out.println(result);
    }
    // other methods ...
}
```

## 4. 提交异步命令执行任务
异步命令不需要注册线程池，异步执行所需线程池在注册命令处理器时指定。

### 4.1 一次提交一个异步命令

方式1：使用TaskHandlerFactory创建ITaskHandler对象
```java
class SingleCommandTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        ITaskHandler handler = TaskHandlerFactory.newTaskHandler( () -> {
            // 加法命令
            return new CalculateCommand(context.getA(), Op.ADD, context.getB());
        }, v -> {
            // 设置加法结果
            context.setH1(v);
        });
        return handler;
    }
    // other methods ...
}
```

方式2：使用ITaskSingle<ContextType, CommandReturnType>扩展接口
```java
class SingleCommandTask implements ITaskSingle<MyContext, Integer> {
    @Override
    public ICommandX<Integer> getCommand(MyContext context) {
        return new CalculateCommand(context.getA(), Op.ADD, context.getB());
    }
    @Override
    public void handleResult(MyContext context, Integer result) {
        context.setH1(result);
    }
   // other methods ...
}
```

### 4.2 一次提交多个异步命令

方式1：使用TaskHandlerFactory创建ITaskHandler对象
```java
class MultiplyCommandTask implements ITask<MyContext> {
    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        ITaskHandler handler = TaskHandlerFactory.newTaskHandler( () -> {
            List<ICommandX<Integer>> commandXList = new ArrayList<>();
            // 加法命令
            commandXList.add(new CalculateCommand(context.getA(), Op.ADD, context.getB()));
            // 乘法命令
            commandXList.add(new CalculateCommand(3, Op.MULTIPLY, context.getB()));
            return commandXList;
        }, (result, finished) -> {
            if (result.getId() == 0) {
                // 第一条加法命令
                context.setH1(result.getValue());
            } else if (result.getId() == 1) {
                // 第二条乘法命令
                context.setH2(result.getValue());
            }
            if (finished) {
                // 所有命令执行完毕，打印结果
                System.out.printf("h1: %s, h2: %s\n", context.getH1(), context.getH2());
            }
        });
        return handler;
    }
    // other methods ...
}
```

方式2：使用ITaskMultiple<ContextType, CommandReturnType>扩展接口
```java
class MultiplyCommandTask2 implements ITaskMultiple<MyContext, Integer> {
    @Override
    public List<ICommandX<Integer>> getCommands(MyContext context) {
        List<ICommandX<Integer>> commandXList = new ArrayList<>();
        // 加法命令
        commandXList.add(new CalculateCommand(context.getA(), Op.ADD, context.getB()));
        // 乘法命令
        commandXList.add(new CalculateCommand(3, Op.MULTIPLY, context.getB()));
        return commandXList;
    }
    @Override
    public void handleResult(MyContext context, IResult<Integer> result, boolean finished) {
        if (result.getId() == 0) {
            // 第一条加法命令
            context.setH1(result.getValue());
        } else if (result.getId() == 1) {
            // 第二条乘法命令
            context.setH2(result.getValue());
        }
        if (finished) {
            // 所有命令执行完毕，打印结果
            System.out.printf("h1: %s, h2: %s\n", context.getH1(), context.getH2());
        }
    }
   // other methods ...
}
```

## 总结
1. 对于使用TaskHandlerFactory提供的工厂方法来创建ITaskHandler对象的方式，
   或是实现ITaskSingle和ITaskMultiple接口的方式，大部分场景都可以相互转化，可以根据个人偏好进行选择。

2. 如果命令结果的处理和命令对象的创建逻辑有一定的依赖关系，则建议使用TaskHandlerFactory提供的工厂方法。比如下面这个例子：
```java
class MultiplyCommandTask2 implements ITask<MyContext> {

    @Override
    public ITaskHandler createTaskHandler(MyContext context) {
        
        List<ICommandX<Integer>> commands = new ArrayList<>();
        Map<Integer, AddItem> itemOfIndex = new HashMap<>(); // 记录命令索引与AddItem的对应关系
        int index = 0;
        for (AddItem item : context.getAdds()) {
            // 使用AddItem对象创建加法命令
            commands.add(new CalculateCommand(item.getA(), Op.ADD, item.getB()));
            // 记录命令索引与AddItem的对应关系
            itemOfIndex.put(index, item);
            index++;
        }
        
        // 使用TaskHandlerFactory创建ITaskHandler对象
        return TaskHandlerFactory.newTaskHandler(() -> {
            return commands;
        }, (result, finished) -> {
            // 获取根据命令索引获取AddItem对象
            AddItem item = itemOfIndex.get(result.getId());
            // 设置item的计算结果
            item.setResult(result.getValue()); 
            if (finished) {
                // 所有命令执行完毕，打印所有结果
                itemOfIndex.forEach((k, v) -> {
                    System.out.printf("%s + %s = %s\n", v.getA(), v.getB(), v.getResult());
                });
            }
        });
    }
    // other methods...
}

@Getter
@Setter
class MyContext implements IFlowContext {
    Set<AddItem> adds;
}

@Setter
@Getter
class AddItem {
    int a;
    int b;
    int result;
}

```