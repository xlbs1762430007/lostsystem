# 第7章 案例实战：AI招领助手开发 V1 知识点总结

> 整理说明：本笔记根据第 7 章视频标题、当前 `lostsystem` 项目源码、提示词文件和配置文件整理。由于视频目录中未发现字幕文件，且本机没有可用的视频字幕提取工具，这不是逐字稿，而是面向学习复习的知识点文档。

## 1. 本章目标

本章围绕“失物招领智能助手”做一个 LangChain4j + Spring Boot 实战项目 V1。核心目标不是只调用一次大模型，而是把大模型能力嵌入业务流程：

- 识别用户意图：判断用户是“丢失信息登记”“找到失物登记”“失物查询”还是“其他问题”。
- 让大模型按结构化对象返回登记结果，而不是只返回一段自然语言。
- 将完整的失物登记信息保存到 MySQL。
- 为多轮对话增加会话记忆，支持补充信息、修改信息等连续场景。
- 通过 LangChain4j Tool 让大模型主动调用业务查询工具。
- 用 AOP 统一保存用户和 AI 的聊天记录。
- 提供前端页面和接口，完成一个可演示的失物招领助手。

## 2. 项目整体架构

项目采用典型分层结构：

- `controller`：暴露 HTTP 接口，例如 `/ai/chat`、`/ai/chat-stream`、`/ai/chat-history`。
- `service`：编排 AI 对话流程，先识别意图，再调用对应业务能力。
- `aiservice`：定义 LangChain4j 的 `@AiService` 接口。
- `aioutput`：定义大模型结构化输出对象。
- `tools`：定义可被大模型调用的业务工具。
- `entity`：定义 JPA 数据库实体。
- `repository`：定义数据访问接口。
- `aop`：用切面统一记录聊天历史。
- `resources`：存放提示词、数据库脚本、页面模板和配置。

整体调用链可以理解为：

```text
前端页面/HTTP 请求
  -> AiController
  -> AiChatServiceImpl.chatStream(userId, message)
  -> AiIntentionAssistant 判断用户意图
  -> 按意图分发到具体业务
  -> AiAssistant / Tool / Repository
  -> 返回 AI 回复
  -> ChatFlowAop 保存用户消息和 AI 消息
```

## 3. 环境与依赖搭建

项目基于：

- Java 17
- Spring Boot
- LangChain4j
- DashScope / 通义千问模型
- Reactor，用于流式输出
- MySQL，用于业务数据与聊天记录持久化
- Spring Data JPA / MyBatis-Plus 相关依赖
- Thymeleaf，用于页面模板

`pom.xml` 中关键依赖：

- `langchain4j-community-dashscope-spring-boot-starter`：接入阿里云百炼 / 通义千问模型。
- `langchain4j-spring-boot-starter`：启用 LangChain4j 与 Spring Boot 集成。
- `langchain4j-reactor`：支持 `Flux<String>` 流式响应。
- `spring-boot-starter-web`：提供传统 MVC 接口。
- `spring-boot-starter-webflux`：支持响应式流式返回。
- `spring-boot-starter-data-jpa`：提供 JPA 实体和仓储。
- `mysql-connector-j`：连接 MySQL。

`application.yaml` 中关键配置：

- 服务端口：`8087`
- 数据库：`jdbc:mysql://localhost:3306/langchain4j`
- 模型：`qwen-max`
- LangChain4j 配置了普通聊天模型和流式聊天模型。

学习重点：

- 先把 Spring Boot 基础工程跑起来，再接入模型。
- 模型配置和业务代码分离，模型名称、API Key、数据库连接都放在配置文件。
- 实际项目中不要把 API Key 明文提交到代码仓库，应该改为环境变量或本地私密配置。

## 4. 第 7 章分集知识点

### 7-1 项目介绍：失物招领智能助手功能介绍

这一节主要明确项目要做什么。

系统面向校园、公司、社区等场景，帮助用户完成失物招领相关操作。用户不需要填写复杂表单，只需要像聊天一样描述：

- “我丢了一个黑色钱包”
- “我捡到一串钥匙”
- “帮我查一下有没有人捡到我的手机”
- “我刚才登记的信息里再补充一下，手机壳是蓝色的”

V1 的重点是打通基础闭环：

- 对话入口
- 意图识别
- 失物登记
- 数据持久化
- 会话记忆
- 聊天历史
- 修改和补充信息

### 7-2 架构设计：失物招领智能助手架构设计

架构设计的核心思想是“AI 只负责理解和生成，业务状态要由后端控制”。

不能把所有逻辑都交给大模型自由发挥，否则会出现：

- 意图不稳定
- 返回格式不可控
- 业务数据无法可靠落库
- 多轮对话上下文容易丢失
- 修改登记时可能产生幻觉

因此项目拆成几个能力：

- `AiIntentionAssistant`：专门做意图识别。
- `AiAssistant`：负责失物登记等领域对话。
- `AiChatServiceImpl`：负责流程编排。
- `LostRegisterRepository`：负责失物登记数据查询和保存。
- `ChatHistoryRepository`：负责聊天历史查询、保存、删除。
- `ChatHistoryTools` / `LostRegisterTools`：把数据库查询能力暴露给大模型。

关键设计点：

- 意图识别和业务处理分开，避免一个 `AiService` 混用多个系统提示词。
- 结构化输出对象承担“模型输出契约”的作用。
- Tool 调用负责补齐模型缺少的外部数据访问能力。
- AOP 负责统一记录聊天过程，避免每个接口重复写保存逻辑。

### 7-3 基础搭建：失物招领基础环境工程搭建

这一节主要搭建项目骨架：

- 创建 Spring Boot 项目。
- 引入 LangChain4j、DashScope、Web、WebFlux、JPA、MySQL、Lombok 等依赖。
- 配置服务端口、数据库、模型名称、API Key。
- 创建基础包结构。

项目入口是：

```java
com.example.lostsystem.LostsystemApplication
```

实体的公共字段放在 `BaseEntity`：

- `id`
- `createdDate`
- `updatedDate`

并通过 JPA 审计注解自动维护创建时间和更新时间。

### 7-4 基础测试：失物招领智能大模型基础环境测试

这一节验证大模型是否能被项目成功调用。

`AiAssistant` 使用 `@AiService` 声明 AI 服务：

```java
@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "qwenChatModel",
    chatMemoryProvider = "chatMemoryProvider",
    streamingChatModel = "qwenStreamingChatModel",
    tools = {"chatHistoryTools", "lostRegisterTools"}
)
```

关键点：

- `wiringMode = EXPLICIT`：显式指定使用哪个模型、记忆提供者和工具。
- `chatModel`：普通聊天模型。
- `streamingChatModel`：流式聊天模型。
- `chatMemoryProvider`：会话记忆提供者。
- `tools`：注册给大模型可调用的工具。

控制器中提供两个入口：

- `/ai/chat`：普通字符串返回。
- `/ai/chat-stream`：`Flux<String>` 流式返回。

学习重点：

- 先测试简单问答，确认模型和接口能跑。
- 再逐步加入提示词、结构化输出、数据库等能力。

### 7-5 核心开发：开发失物招领大模型用户意图功能

用户输入首先要被分流，项目用 `AiIntentionAssistant` 专门做意图识别。

对应提示词文件是 `getIntention.txt`，要求模型判断四类意图：

- `1`：丢失信息登记。
- `2`：找到失物登记。
- `3`：失物查询。
- `4`：其他。

对应结构化输出类是 `IntentionOutput`：

```java
private Integer intention;
private String output;
```

`AiChatServiceImpl.chatStream` 中先调用：

```java
IntentionOutput intention = aiIntentionAssistant.intention(userId, message);
```

再根据 `intention.getIntention()` 进行 `switch` 分发。

学习重点：

- “意图识别”是 AI 应用编排的第一步。
- 让大模型返回结构化对象，比靠字符串包含判断更稳定。
- 意图识别的输出要足够简单，便于后端代码决策。

### 7-6 阶段测试：失物招领用户意图阶段自测

这一节主要验证意图识别效果。

建议用例：

- “我丢了一部手机” -> 应识别为 `1`。
- “我捡到一个钱包” -> 应识别为 `2`。
- “帮我查一下有没有人捡到学生卡” -> 应识别为 `3`。
- “给我讲个笑话” -> 应识别为 `4`。

测试重点：

- 用户表达不标准时，模型是否仍能分类。
- 与失物招领无关的问题是否能拒答。
- 输出字段是否能稳定映射到 `IntentionOutput`。

### 7-7 核心开发：开发失物招领大模型失物登记功能

这一节实现“丢失信息登记”。

对应提示词文件是 `registerLost.txt`，它要求模型扮演失物登记专员，完成：

- 提取用户姓名。
- 提取用户手机号。
- 提取失物名称。
- 提取失物特征。
- 判断登记是否完成。
- 如果信息不完整，继续追问缺失项。
- 如果用户明确没有更多特征，也允许完成登记。

对应结构化输出类是 `LostRegisterOutput`：

```java
private String output;
private String username;
private String phone;
private String lostName;
private String lostType;
private Boolean completed;
private Long id;
```

在 `AiAssistant` 中，登记方法定义为：

```java
@UserMessage("当前sessionId:{{sessionId}}; 用户当前消息: {{message}}")
LostRegisterOutput registerLost(@V("sessionId") String sessionId, @V("message") String message);
```

学习重点：

- `@UserMessage` 可以把业务参数注入到提示词模板中。
- `@V` 用于给模板变量赋值。
- 结构化输出类字段上的 `@Description` 是给模型看的字段说明。

### 7-8 引出问题：多个系统提示词共用 AiService 带来的问题

这一节讨论一个常见坑：不同任务共用同一个 `AiService`，但系统提示词不同，会让模型角色和目标混乱。

例如：

- 意图识别需要模型“只做分类”。
- 失物登记需要模型“收集信息并输出登记结果”。
- 查询、招领、修改又可能是别的角色和约束。

如果都塞进一个服务里，容易出现：

- 该分类时却开始追问登记信息。
- 该登记时却输出意图说明。
- 多个任务的提示词互相污染。
- 结构化输出不稳定。

本项目的做法是拆分：

- `AiIntentionAssistant` 使用 `getIntention.txt`。
- `AiAssistant` 使用 `registerLost.txt`。

学习重点：

- 一个 `AiService` 尽量对应一个清晰角色和任务。
- 系统提示词的边界越清晰，模型输出越可控。

### 7-9 阶段测试：大模型失物登记功能测试

这一节测试登记流程。

测试时要关注：

- 信息齐全时能否一次完成登记。
- 缺少姓名或手机号时能否追问。
- 缺少物品特征时能否引导用户补充。
- 用户说“没有更多信息”时能否完成登记。
- `completed` 为 `true` 时，后端是否保存数据库。

`AiChatServiceImpl.registerLost` 中的核心逻辑：

```java
LostRegisterOutput lostRegisterOutput = aiAssistant.registerLost(userId, message);
if (Boolean.TRUE.equals(lostRegisterOutput.getCompleted())) {
    LostRegisterEntity entity = new LostRegisterEntity();
    entity.setUsername(lostRegisterOutput.getUsername());
    entity.setPhone(lostRegisterOutput.getPhone());
    entity.setLostName(lostRegisterOutput.getLostName());
    entity.setLostType(lostRegisterOutput.getLostType());
    lostRegisterRepository.save(entity);
}
return lostRegisterOutput.getOutput();
```

学习重点：

- AI 的自然语言回复和后端业务数据要分开处理。
- 只有模型明确判定 `completed = true`，才落库。
- 外层要捕获解析异常，避免模型输出异常导致接口崩溃。

### 7-10 持久化数据：开发失物登记数据持久化数据库

这一节把登记结果保存到数据库。

失物登记实体是 `LostRegisterEntity`，对应表 `lost_register`：

- `username`：用户姓名。
- `phone`：用户手机号。
- `lostName`：失物名称。
- `lostType`：失物特征。
- 公共字段继承自 `BaseEntity`。

仓储接口：

```java
public interface LostRegisterRepository extends BaseRepository<LostRegisterEntity> {
    List<LostRegisterEntity> findAllByPhone(String phone);
}
```

其中 `BaseRepository` 继承 `CrudRepository<T, Long>`。

学习重点：

- 模型负责抽取信息，Repository 负责持久化。
- 业务表字段应该尽量结构化，不要只存一整段模型文本。
- 通过手机号查询历史登记，是后续“补充/修改”功能的基础。

### 7-11 阶段测试：失物登记持久化数据库测试

这一节验证数据库保存是否正确。

测试重点：

- 提供完整信息后，`lost_register` 是否新增记录。
- 字段是否拆分正确：姓名、手机号、物品名称、物品特征。
- 多轮补充后，最终保存的信息是否包含上下文。
- 不完整信息不应该提前保存。

建议测试语句：

```text
我丢了一个黑色钱包，里面有身份证，我叫张三，手机号 13800000000
```

预期：

- `completed = true`
- 返回“已完成登记”一类提示
- 数据库出现一条记录

### 7-12 核心开发：开发失物招领会话记忆 AOP 切面功能

这一节实现聊天记录持久化。

项目定义了注解：

```java
@ChatFlow
```

在 `AiChatServiceImpl.chatStream` 上使用该注解后，`ChatFlowAop` 会自动拦截方法调用。

切面逻辑：

1. 从方法参数中取 `sessionId/userId` 和用户消息。
2. 先保存用户消息，角色为 `0`。
3. 执行业务方法。
4. 如果结果是 `Flux`，拼接流式返回的所有片段。
5. 流结束后保存 AI 回复，角色为 `1`。
6. 如果结果不是 `Flux`，直接保存结果文本。

学习重点：

- AOP 适合处理横切逻辑，例如日志、审计、聊天记录。
- 流式响应不能一开始就拿到完整 AI 回复，需要在 `doOnNext` 中累积，在 `doOnComplete` 中保存。
- 保存聊天记录不要散落在多个业务方法里，否则后期维护困难。

### 7-13 核心开发：开发自定义会话聊天记忆 Tool 工具类

这一节把聊天历史查询封装成 LangChain4j Tool。

`ChatHistoryTools` 中定义：

```java
@Tool(name = "获取用户聊天历史对话")
public List<ChatHistory> getChatHistory(@P("sessionId") String sessionId)
```

大模型在需要理解上下文时，可以调用这个工具拿到当前用户历史对话。

提示词 `registerLost.txt` 明确要求：

- 每次对话都通过工具读取用户历史对话内容。
- 要结合历史记录自然承接上下文。
- 禁止机械重复话术。

学习重点：

- LangChain4j Tool 是让大模型调用后端能力的桥。
- `@Tool` 描述要写清楚，模型才知道什么时候调用。
- `@P` 参数说明也很重要，能降低模型传错参数的概率。

### 7-14 阶段测试：测试自定义会话聊天记忆功能

这一节测试“多轮对话能否记住前文”。

示例流程：

```text
用户：我丢了一部手机
AI：请补充姓名、手机号、手机特征
用户：我叫李雷，手机号 13800000000
AI：请补充手机颜色、型号等特征
用户：黑色 iPhone，蓝色手机壳
AI：已完成登记
```

测试重点：

- 第二轮、第三轮是否能继承第一轮的“手机”信息。
- AI 是否不会反复追问已经提供过的信息。
- 最终落库信息是否合并了多轮内容。

### 7-15 核心开发：开发失物登记修改功能

这一节处理“已登记后继续补充或修改”的场景。

项目的思路是：

- 用户输入手机号后，模型可调用 `LostRegisterTools.queryLostRegisterByPhone` 查询已有登记。
- 如果查到历史登记，结合用户当前意图判断是新增登记还是补充/修改旧登记。
- 如果用户是在补充旧信息，需要围绕已有记录继续追问或更新。

当前源码中 `LostRegisterOutput` 已预留：

```java
private Long id;
```

这个字段可用于标识要修改的数据库记录。

学习重点：

- 修改业务不能只靠自然语言记忆，要定位具体数据库记录。
- 多条记录时，需要让用户确认修改哪一条。
- 大模型可以辅助判断，但最终更新哪个 `id` 应由后端明确控制。

### 7-16 阶段测试：测试失物登记修改功能

这一节验证补充和修改流程。

建议测试场景：

- 已登记手机号后，再说“我补充一下，我的钱包里有校园卡”。
- 同一个手机号有多条登记时，让用户选择要补充哪条。
- 用户表达不明确时，不应直接修改数据库。

测试重点：

- 是否能查到历史记录。
- 是否能正确识别“补充信息”而不是“新增登记”。
- 是否避免重复插入一条新记录。

### 7-17 场景优化：解决修改失物登记场景大模型出现幻觉问题

这一节关注大模型幻觉问题。

在修改登记时，模型容易凭空补全：

- 未提供的颜色、型号、地点。
- 错误的手机号。
- 错误的失物名称。
- 不存在的历史登记。

优化方向：

- 提示词中明确“只能基于用户输入和工具查询结果处理”。
- 查不到历史记录时，必须告诉用户未查到，而不是假装存在。
- 多条数据时必须让用户确认，不要擅自选择。
- 更新数据库前用 `id` 锁定记录。
- 后端对关键字段做校验，不完全信任模型输出。

学习重点：

- 大模型很适合理解语言，但不适合单独作为事实来源。
- 数据库查询结果才是业务事实。
- 越靠近“写库”的操作，越要后端收紧约束。

### 7-18 完善功能：完善失物招领助手会话记录查询和删除记录功能

这一节完善聊天记录接口。

`ChatHistoryController` 提供：

- `GET /ai/chat-history?userId=xxx`：查询某个用户的聊天记录。
- `DELETE /ai/chat-history?userId=xxx`：删除某个用户的聊天记录。

查询时按创建时间升序：

```java
findBySessionIdOrderByCreatedDateAsc(userId)
```

返回对象 `ChatHistory` 包含：

- `role`
- `content`
- `createdDate`

学习重点：

- 聊天记录对前端回显很重要。
- 清空记录可以帮助用户重新开始一个干净会话。
- 历史记录表最好加 `session_id` 和 `created_date` 索引。

### 7-19 核心开发：开发找到失物登记大模型功能

这一节开始扩展“我捡到了东西”的登记能力。

它和“我丢了东西”的逻辑类似，但业务语义不同：

- 丢失登记：失主登记自己丢了什么。
- 找到登记：拾到者登记自己捡到了什么。

找到失物登记通常要收集：

- 拾到者姓名或联系方式。
- 找到的物品名称。
- 物品特征。
- 拾到地点。
- 拾到时间。
- 认领方式或存放地点。

当前源码中第 7 章主要已经完成丢失登记基础，找到失物登记在 `AiChatServiceImpl` 的 `case 2` 中留了入口：

```java
case 2:
    // 找到失物登记
    break;
```

学习重点：

- 业务意图分支应该提前设计好，即使暂时只实现其中一部分。
- “找到登记”和“丢失登记”不要混在同一张表或同一套提示词里，除非字段模型能够清晰区分。
- 找到登记同样适合用结构化输出对象承接模型结果。

### 7-20 核心开发：开发找到失物登记业务功能

这一节落地“找到失物登记”的后端业务。

合理实现方式：

- 新建 `FoundRegisterEntity` 或在现有表增加类型字段。
- 新建 `FoundRegisterOutput` 承接大模型输出。
- 新建对应 `Repository`。
- 在意图为 `2` 时调用找到登记流程。
- 信息完整后保存数据库。

学习重点：

- 同类业务可以复用模式，但不要盲目复制粘贴。
- 如果两类登记字段差异明显，分表更清晰。
- 如果后续要做失物匹配，丢失登记和找到登记都要有可比较字段，例如物品名称、特征、时间、地点。

### 7-21 阶段测试：测试与优化找到失物登记相关功能点

这一节测试找到登记功能。

建议测试用例：

- “我在图书馆捡到一张校园卡”
- “我捡到一个黑色保温杯，放在前台了”
- “我刚才说的保温杯是在三楼捡到的”

测试重点：

- 是否能识别为找到失物登记。
- 是否能追问缺失信息。
- 是否能保存拾到物品的关键字段。
- 是否不会错误走到“丢失登记”分支。

### 7-22 阶段测试：找到失物补充信息功能测试

这一节验证找到登记的多轮补充。

重点和丢失登记类似：

- 能否结合历史对话。
- 能否识别用户是在补充已有找到登记。
- 能否避免重复登记。
- 能否在缺少关键字段时继续追问。

学习重点：

- 多轮补充的关键是：会话历史 + 数据库历史 + 明确业务状态。
- 仅依赖模型聊天记忆不够，重要业务状态要持久化。

### 7-23 总结优化：阶段性总结 & 优化版本说明

这一节对 V1 做阶段性总结。

V1 已经具备：

- 基础前端页面。
- AI 聊天接口。
- 流式响应。
- 意图识别。
- 丢失登记。
- 数据库存储。
- 会话记忆。
- 聊天记录查询和删除。
- Tool 调用业务数据。
- 找到登记相关能力的开发和测试。

后续可优化方向：

- 增加“失物查询”和“失物匹配”。
- 增加向量检索，根据特征相似度匹配丢失物和拾到物。
- 增加人工确认流程，防止模型误写库。
- 将 API Key 移出配置文件。
- 增加单元测试和集成测试。
- 增加字段校验，例如手机号格式、空字段检查。
- 优化会话记忆，目前 `InMemoryChatMemoryStore` 重启后会丢失。
- 对聊天历史设置分页或只取最近 N 条，避免上下文过长。

## 5. LangChain4j 核心知识点

### 5.1 `@AiService`

`@AiService` 可以把 Java 接口变成 AI 服务代理。开发者只需要定义方法，LangChain4j 负责调用模型。

本项目有两个 AI 服务：

- `AiIntentionAssistant`：做意图识别。
- `AiAssistant`：做失物登记和通用对话。

这样做的好处：

- 每个服务的系统提示词更清晰。
- 输出对象更稳定。
- 后端流程更容易维护。

### 5.2 `@SystemMessage`

`@SystemMessage(fromResource = "...")` 从资源文件加载系统提示词。

本项目中：

- `getIntention.txt`：意图识别提示词。
- `registerLost.txt`：失物登记提示词。

学习建议：

- 复杂提示词放到 `.txt` 文件里，别全部写在注解字符串里。
- 一个提示词只服务一个明确任务。
- 提示词中要写清楚角色、任务、字段、限制、异常场景。

### 5.3 `@UserMessage` 和 `@V`

`@UserMessage` 用于定义用户消息模板，`@V` 用于填充模板变量。

示例：

```java
@UserMessage("当前sessionId:{{sessionId}}; 用户当前消息: {{message}}")
LostRegisterOutput registerLost(@V("sessionId") String sessionId, @V("message") String message);
```

这样模型能同时知道：

- 当前用户是谁。
- 当前输入是什么。
- 后续 Tool 调用应该使用哪个 sessionId。

### 5.4 `@MemoryId`

`@MemoryId` 用于区分不同用户或不同会话的聊天记忆。

示例：

```java
String chat(@MemoryId String id, @UserMessage String message);
```

本项目把 `userId` 当作会话 ID 使用。

学习重点：

- 多用户系统必须隔离记忆。
- 不同用户不能共享同一个 memory id。
- 业务上更推荐用真正的 `sessionId`，而不是直接用用户 ID。

### 5.5 结构化输出

`IntentionOutput` 和 `LostRegisterOutput` 都是结构化输出类。

结构化输出的好处：

- 后端可以直接读取字段。
- 方便做分支判断。
- 方便落库。
- 比解析自然语言更稳定。

注意：

- 字段描述要写清楚。
- 布尔字段、枚举字段要约束明确。
- 写库前仍要做后端校验。

### 5.6 Tool 调用

Tool 让模型能调用后端方法。

本项目中的工具：

- `ChatHistoryTools.getChatHistory`：查询用户聊天历史。
- `LostRegisterTools.queryLostRegisterByPhone`：根据手机号查询丢失登记。
- `TestTools`：演示工具调用，例如获取班级、天气。

Tool 适合用于：

- 查询数据库。
- 调用业务服务。
- 获取外部系统数据。
- 做模型自己无法可靠完成的事实性操作。

不适合完全交给 Tool 的部分：

- 是否写库。
- 是否删除数据。
- 是否修改关键业务状态。

这些操作最好由后端流程确认后执行。

## 6. 会话记忆与聊天历史

项目中有两层“记忆”概念：

### 6.1 LangChain4j 内存记忆

`ChatMemoryConfig` 中使用：

```java
MessageWindowChatMemory.builder()
    .id(id)
    .maxMessages(20)
    .chatMemoryStore(chatMemoryStore)
    .build();
```

并且当前 `ChatMemoryStore` 是：

```java
new InMemoryChatMemoryStore()
```

它的作用是让模型在当前服务运行期间记住最近 20 条消息。

局限：

- 服务重启后丢失。
- 只适合演示或轻量场景。
- 不等同于数据库里的业务历史。

### 6.2 数据库聊天历史

`ChatFlowAop` 会把用户输入和 AI 输出保存到 `chat_history` 表。

字段：

- `session_id`
- `role`
- `content`
- `created_date`
- `updated_date`

这部分可以用于：

- 前端历史回显。
- 给大模型 Tool 查询。
- 问题排查。
- 后续做审计和分析。

学习重点：

- 内存记忆解决“模型上下文”问题。
- 数据库历史解决“业务可追踪”问题。
- 两者不是一回事，但可以配合使用。

## 7. 失物登记业务流程

完整流程如下：

```text
用户发送消息
  -> AOP 保存用户消息
  -> 意图识别
  -> 判断为丢失登记
  -> 调用失物登记 AiService
  -> 模型读取历史对话 / 查询手机号历史记录
  -> 模型抽取结构化字段
  -> 后端检查 completed
  -> completed = true 时保存 lost_register
  -> 返回 output 给用户
  -> AOP 保存 AI 消息
```

关键字段：

- `username`：谁丢了。
- `phone`：联系方式。
- `lostName`：丢了什么，应该是简短名称。
- `lostType`：具体特征，例如颜色、材质、尺寸、特殊标识。
- `completed`：登记是否完成。
- `id`：用于后续修改已有记录。

## 8. 常见问题与避坑

### 8.1 不要让一个提示词承担太多任务

意图识别、失物登记、找到登记、查询匹配最好拆开。提示词越复杂，模型越容易跑偏。

### 8.2 不要完全相信模型输出

模型可能会：

- 编造字段。
- 误判是否完成。
- 把用户未说的信息补出来。
- 选错要修改的记录。

后端要对关键字段做校验。

### 8.3 流式响应保存历史要等流结束

`Flux<String>` 是一段一段返回的，不能直接把 `Flux` 转字符串保存。

正确思路：

- `doOnNext` 累积每个片段。
- `doOnComplete` 保存完整 AI 回复。

### 8.4 业务事实应来自数据库

模型可以理解“用户想补充登记”，但“之前登记了什么”应该来自数据库查询结果。

### 8.5 API Key 不要写死

当前配置文件中存在明文 API Key。真实项目中应改成：

```yaml
api-key: ${DASHSCOPE_API_KEY}
```

再通过环境变量注入。

## 9. 建议学习顺序

如果你想不看视频、直接读代码学习，建议按这个顺序：

1. 看 `pom.xml` 和 `application.yaml`，理解项目依赖和模型配置。
2. 看 `AiController`，知道接口入口。
3. 看 `AiChatServiceImpl`，理解业务编排主流程。
4. 看 `AiIntentionAssistant` 和 `getIntention.txt`，理解意图识别。
5. 看 `AiAssistant` 和 `registerLost.txt`，理解提示词和结构化登记。
6. 看 `IntentionOutput`、`LostRegisterOutput`，理解模型输出契约。
7. 看 `LostRegisterEntity`、`LostRegisterRepository`，理解登记数据持久化。
8. 看 `ChatFlowAop`、`ChatHistoryEntity`、`ChatHistoryRepository`，理解聊天历史保存。
9. 看 `ChatHistoryTools`、`LostRegisterTools`，理解 Tool 如何把数据库能力交给模型。
10. 最后看 `index.html`，理解前端如何调用后端流式接口和历史记录接口。

## 10. 本章你需要真正掌握的能力

学完这一章，应掌握：

- 如何在 Spring Boot 中接入 LangChain4j。
- 如何配置 DashScope / 通义千问模型。
- 如何用 `@AiService` 定义 AI 服务接口。
- 如何用系统提示词控制模型角色和任务边界。
- 如何让模型返回 Java 结构化对象。
- 如何用意图识别做业务路由。
- 如何把模型抽取出的业务字段保存到数据库。
- 如何用 AOP 保存聊天记录。
- 如何用 Tool 让模型查询业务数据。
- 如何处理多轮对话中的补充、修改和上下文问题。
- 如何识别并缓解模型幻觉。

## 11. 当前项目对应文件速查

- `src/main/java/com/example/lostsystem/controller/AiController.java`：AI 聊天接口。
- `src/main/java/com/example/lostsystem/controller/ChatHistoryController.java`：聊天历史查询和删除接口。
- `src/main/java/com/example/lostsystem/service/impl/AiChatServiceImpl.java`：AI 对话主流程编排。
- `src/main/java/com/example/lostsystem/aiservice/AiIntentionAssistant.java`：意图识别 AI 服务。
- `src/main/java/com/example/lostsystem/aiservice/AiAssistant.java`：失物登记 AI 服务。
- `src/main/java/com/example/lostsystem/aioutput/IntentionOutput.java`：意图识别结构化输出。
- `src/main/java/com/example/lostsystem/aioutput/LostRegisterOutput.java`：失物登记结构化输出。
- `src/main/resources/getIntention.txt`：意图识别提示词。
- `src/main/resources/registerLost.txt`：失物登记提示词。
- `src/main/java/com/example/lostsystem/entity/LostRegisterEntity.java`：丢失登记实体。
- `src/main/java/com/example/lostsystem/entity/ChatHistoryEntity.java`：聊天历史实体。
- `src/main/java/com/example/lostsystem/repository/LostRegisterRepository.java`：丢失登记数据访问。
- `src/main/java/com/example/lostsystem/repository/ChatHistoryRepository.java`：聊天历史数据访问。
- `src/main/java/com/example/lostsystem/tools/ChatHistoryTools.java`：聊天历史 Tool。
- `src/main/java/com/example/lostsystem/tools/LostRegisterTools.java`：手机号查询丢失登记 Tool。
- `src/main/java/com/example/lostsystem/chatmemory/ChatMemoryConfig.java`：LangChain4j 会话记忆配置。
- `src/main/resources/templates/index.html`：前端页面。

## 12. 快速复习版

一句话总结本章：

> 本章用 Spring Boot + LangChain4j 做了一个失物招领助手，通过意图识别决定业务分支，通过结构化输出提取登记字段，通过数据库保存业务数据，通过 AOP 保存聊天记录，通过 Tool 让模型查询历史记录，从而支持多轮登记、补充和修改。

最重要的三条主线：

1. AI 服务设计：`@AiService` + `@SystemMessage` + `@UserMessage` + 结构化输出。
2. 业务流程编排：先识别意图，再进入登记、查询、找到登记等分支。
3. 状态管理：聊天记忆负责上下文，数据库负责真实业务数据。

最容易踩坑的三点：

1. 多个系统提示词混用同一个 `AiService`，导致模型角色混乱。
2. 直接相信模型输出并写库，导致幻觉数据污染数据库。
3. 只依赖内存记忆，不保存聊天历史，导致重启或刷新后上下文丢失。
