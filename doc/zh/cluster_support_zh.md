# 分布式任务支持

## 分布式任务介绍

使多个独立的任务实例组合为一个集群，一个流程的多个任务可以被分配到集群中任意的实例执行。分布式集群的作用是充分利用集群资源（CPU、内存），
避免集群中有些实例资源比较紧张，而有些实例资源比较空闲。

分布式任务的应用场景：

1. 流程任务比较多
2. 流程要处理的数据量比较大

## 开始

要支持任务分布式任务，无需修改流程和任务代码，只需要做2步配置即可，分别是：

1. 引入terse-cluster-support组件
2. 提供集群配置

### 第一步：maven引入

```xml

<dependency>
    <groupId>io.github.chyohn.terse</groupId>
    <artifactId>terse-cluster-support</artifactId>
    <version>${terse.version}</version>
</dependency>
```

### 第二步：提供集群配置

#### 非spring应用

通过代码提供配置，代码示例如下：
 ```java
// 启动时配置
Map<String, Object> config = new HashMap<>();
config.put("cluster.seed.nodes", "127.0.0.1:9001,127.0.0.1:9002");                                                                      
config.put("cluster.port", "9001");
Terse.initCluster(config);

// 应用可以对外提供服务时才执行，过早执行会找不到服务执行
Terse.readyCluster();
 ```

#### spring应用
按文档[与Spring和Spring boot集成](spring_support_zh.md)说明配置，然后在spring配置文件加入集群相关配置，如配置文件`application.yaml`，配置示例如下：
 ```yaml
 cluster:
     seed:
       # seed nodes, format: ip:port,ip:port,...
       nodes: 27.0.0.1:9001,127.0.0.1:9002
     # listen port for server
     port: 9001
 ```

## 配置项说明

| 配置项                | 描述                                               | 默认值  |
|--------------------|--------------------------------------------------|------|
| cluster.seed.nodes | 种子节点，当前实例通过种子节点加入集群。必填项。 格式为：ip:port,ip:port,... | 无    |
| cluster.port       | 如果`serverEnabled=true`,该配置项必填。表示当前节点的服务端口号       | 9001 |
