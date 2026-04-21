# Backend 项目细节

##  项目概述

本项目是一个基于 **Spring Boot 4.0** 和 **MyBatis** 构建的现代化后端应用，采用 Java 17 作为开发语言，提供了高性能、可扩展的 RESTful API 服务。

### 技术栈

- **核心框架**: Spring Boot 4.0.5
- **Web 框架**: Spring WebMVC
- **持久层框架**: MyBatis 4.0.1
- **数据库**: MySQL（已配置，暂未启用）
- **构建工具**: Maven
- **Java 版本**: Java 17
- **代码格式化**: Spotless

---

##  项目结构
````
backend/ 
│ ├──  pom.xml # Maven 项目配置文件 
├──  mvnw.cmd # Maven Wrapper（Windows） 
├──  HELP.md # Spring Boot 帮助文档 
├──  README.md # 项目说明文档 
│ ├──  src/ # 源代码目录 
│ │ │ ├──  main/ # 主代码目录 
│ │ │ │ │ ├──  java/ # Java 源代码 
│ │ │ │ │ │ │ ├──  com/example/backend/ 
│ │ │ │ │ │ │ │ │ ├──  BackendApplication.java # Spring Boot 启动类 
│ │ │ │ │ │ │ │ │ └──  HelloController.java # 示例 REST 控制器 
│ │ │ │ │ └──  resources/ # 资源文件目录
│ │ │ └──  test/ # 测试代码目录 
│ └──  target/ # 编译输出目录（自动生成） 
├──  classes/ # 编译后的 class 文件 
├──  test-classes/ # 编译后的测试 class 文件 
└──  backend-0.0.1-SNAPSHOT.jar # 打包后的可执行 JAR
````



### 核心文件说明

| 文件/目录 | 类型 | 说明 |
|-----------|------|------|
| `pom.xml` | XML | Maven 项目配置，定义依赖和插件 |
| `BackendApplication.java` | Java | Spring Boot 应用启动类，包含 main 方法 |
| `HelloController.java` | Java | REST 控制器示例，提供 `/hello` 接口 |
| `application.properties` | Properties | 应用配置文件，定义应用名称等配置 |
| `BackendApplicationTests.java` | Java | Spring Boot 测试类 |
| `src/main/java/` | Directory | Java 源代码目录 |
| `src/main/resources/` | Directory | 资源文件目录（配置文件、静态资源等） |
| `src/test/java/` | Directory | 测试代码目录 |



---

## 核心技术详解

### 1. Spring Boot 4.0

项目采用最新的 **Spring Boot 4.0.5**，提供自动配置、快速开发和生产级特性。

**启动类 - BackendApplication.java:**
```java
package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// 项目初始化，暂时不使用数据库
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
````
特点:
- 自动配置（Auto Configuration） 
- 内嵌 Tomcat 服务器 
- 生产就绪特性（Actuator） 
- 排除数据源自动配置（当前阶段）
### 2. Spring WebMVC
使用 Spring WebMVC 构建 RESTful API，提供简洁的控制器编程模型。

示例控制器 - HelloController.java:

```java
package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
````
注解说明:

- @RestController: 组合注解（@Controller + @ResponseBody），标识 REST 控制器 
- @GetMapping: 映射 HTTP GET 请求到处理方法 
- 返回值自动序列化为 HTTP 响应体

### 3. MyBatis 集成
项目集成了 MyBatis 4.0.1，提供灵活的 SQL 映射和持久层支持。


4. Maven 构建系统
使用 Maven 作为项目管理和构建工具，提供依赖管理和生命周期管理。

关键配置 - pom.xml:

````xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.5</version>
</parent>

<properties>
    <java.version>17</java.version>
</properties>
````

#### 环境要求
- JDK: >= 17 
- Maven: >= 3.6.0（或使用 Maven Wrapper） 
- IDE: IntelliJ IDEA / Eclipse / VS Code 
- 数据库: MySQL 8.0+（可选，当前未启用）

----
## 开发命令
启动开发服务器
````bash
mvn spring-boot:run
````
或

````bash
./mvnw.cmd spring-boot:run    # Windows
````
**默认端口:** http://localhost:8080

**访问测试:** http://localhost:8080/hello

**构建生产版本**
````bash
mvn clean package
````
输出目录: target/

**生成可执行 JAR**: target/backend-0.0.1-SNAPSHOT.jar

**跳过测试**: mvn clean package -DskipTests

**运行打包后的应用**
```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```
**代码格式化**
```bash
mvn spotless:apply
```
- 自动移除未使用的 import 
- 统一代码格式


**运行测试**
```bash
mvn test
```

> 最后更新: 2026-04-21
