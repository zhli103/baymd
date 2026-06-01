# BayMD — 通用 RAG 应用框架

## 项目定位

BayMD 是一个通用的 RAG（Retrieval-Augmented Generation）应用框架，提供文档解析、向量检索、意图分类、MCP 工具集成、模型路由、分布式限流等可复用基础设施。框架完全与业务领域解耦，可在此基础上构建任意领域的 RAG 应用（如客服助手、知识库问答、企业内部助手等）。

## 模块结构

| 模块 | 说明 |
|------|------|
| `framework` | 基础设施层：分布式限流、AOP 切面、上下文传递、通用工具 |
| `infra-ai` | AI 基础设施：Chat Client 抽象、模型路由、健康检查、流式响应 |
| `mcp-server` | MCP 工具服务：独立的 MCP Server 进程，可注册任意工具 |
| `bootstrap` | 应用启动层：RAG 核心逻辑、知识库管理、文档摄取、API 接口 |

## 技术栈

- **JDK 17**, **Spring Boot 3.5.7**
- **MyBatis-Plus 3.5.14** + **PostgreSQL** (pgvector)
- **Milvus SDK 2.6.6** — 向量数据库
- **Redisson 4.0** — 分布式锁 / 限流
- **RocketMQ** — 异步消息
- **Apache Tika 3.2.3** — 文档解析
- **Sa-Token 1.43.0** — 认证鉴权
- **MCP SDK 1.1.2** — Model Context Protocol 工具集成
- **OkHttp 4.12.0** — HTTP 客户端
- **AWS SDK S3 2.40.2** — 对象存储

## 核心架构

### RAG 对话流水线 (StreamChatPipeline)

7 阶段流水线：
```
Memory Load → Query Rewrite → Intent Resolution → Ambiguity Guidance
→ System-Only Check → Retrieval → Stream Response
```

关键文件：`bootstrap/.../rag/service/pipeline/StreamChatPipeline.java`

### 多通道混合检索 (MultiChannelRetrievalEngine)

并行执行两种检索通道，结果经去重+重排序后返回：
- **VectorGlobalSearchChannel** — 全局向量检索
- **IntentDirectedSearchChannel** — 意图定向检索（仅限意图匹配的知识库）

后处理器：DeduplicationPostProcessor → RerankPostProcessor

关键文件：`bootstrap/.../rag/core/retrieve/`

### 意图分类 (Intent Tree)

树形层级意图分类：`DOMAIN → CATEGORY → TOPIC`，每个节点支持三种类型：
- `KB` (0) — 知识库检索
- `SYSTEM` (1) — 系统直接处理
- `MCP` (2) — 调用 MCP 工具获取实时数据

默认意图树在 `IntentTreeFactory.java` 中定义（可通过数据库动态管理），启动时通过 `initFromFactory()` 写入数据库。

关键文件：
- `bootstrap/.../rag/core/intent/IntentNode.java` — 意图节点模型
- `bootstrap/.../rag/core/intent/IntentTreeFactory.java` — 默认意图树工厂
- `bootstrap/.../rag/core/intent/DefaultIntentClassifier.java` — LLM 意图分类器
- `bootstrap/.../ingestion/service/impl/IntentTreeServiceImpl.java` — 数据库意图树管理

### 模型路由与健康检查 (Model Health & Routing)

**三状态断路器**：`CLOSED → OPEN → HALF_OPEN`
- `ModelHealthStore` — CAS 原子状态转换，单探针信号量
- `ModelRoutingExecutor` — 有序候选列表 → 健康检查 → 首包探活 → 成功提交/失败切换

关键文件：
- `infra-ai/.../ModelHealthStore.java`
- `infra-ai/.../ModelRoutingExecutor.java`

### 分布式公平队列限流 (Fair Queue Rate Limiter)

Redis ZSET + Lua 原子脚本 + RPermitExpirableSemaphore + RTopic Pub/Sub

关键文件：`framework/.../FairDistributedRateLimiter.java`

### 文档摄取 DAG (Ingestion Pipeline)

6 节点流水线，支持条件评估和链表式执行：
```
Fetcher → Parser → Enhancer → Chunker → Enricher → Indexer
```

每个节点独立配置、独立执行。通过 `IngestionEngine` 编排。

关键文件：`bootstrap/.../ingestion/engine/IngestionEngine.java`

### 分块策略 (Chunking Strategy)

Strategy 模式，支持多种分块算法：
- `FixedSizeTextChunker` — 固定大小分块
- `StructureAwareTextChunker` — 结构感知分块（保留 Markdown 结构）

通过 `ChunkingStrategyFactory` 根据配置选择策略。

### 文档解析 (Document Parser)

支持多种文档格式，自动选择合适的解析器：
- `TikaDocumentParser` — PDF, DOC, DOCX, HTML 等（基于 Apache Tika）
- `MarkdownDocumentParser` — Markdown 文件

通过 `DocumentParserSelector` 自动选择。

### 对话记忆 (Conversation Memory)

- 短时记忆：保留最近 N 轮对话历史（`history-keep-turns`）
- 长时摘要：超过 N 轮后自动生成对话摘要（`summary-start-turns`）

支持 JDBC 存储和缓存。

## 设计模式一览

| 模式 | 应用场景 |
|------|----------|
| **Strategy** | ChunkingStrategy, DocumentParser |
| **Template Method** | AbstractOpenAIStyleChatClient |
| **Chain of Responsibility** | StreamChatPipeline 阶段, 检索后处理器 |
| **Registry** | IntentNodeRegistry, MCP 工具注册 |
| **Factory** | ChunkingStrategyFactory, IntentTreeFactory |
| **AOP** | @RagTraceNode (追踪), @Idempotent (幂等) |
| **Circuit Breaker** | ModelHealthStore |
| **Observer** | RocketMQ 事件驱动 (文档更新、缓存失效) |

## 扩展点

### 1. 构建定制化意图树
实现 `IntentTreeFactory.buildIntentTree()` 或通过 API 动态管理数据库意图节点。

### 2. 注册 MCP 工具
在 `mcp-server` 模块中新建 `@Component`，参考 `ExampleMcpExecutor.java` 创建 `SyncToolSpecification` Bean。

### 3. 添加新的分块策略
实现 `ChunkingStrategy` 接口，在 `ChunkingStrategyFactory` 中注册。

### 4. 添加新的文档解析器
实现 `DocumentParser` 接口，在 `DocumentParserSelector` 中注册。

### 5. 添加新的检索通道
继承 `SearchChannel` 基类，注册到 `MultiChannelRetrievalEngine`。

### 6. 添加新的模型供应商
在 `application.yaml` 的 `ai.providers` 下添加供应商配置，在 `ai.chat.candidates` 中声明模型。

### 7. 自定义 Prompt 模板
修改 `bootstrap/src/main/resources/prompt/` 下的 `.st` 模板文件，或通过 `PromptTemplateLoader` 加载自定义路径的模板。

## 配置要点

### 应用配置 (`application.yaml`)

- `rag.vector.type` — 向量存储类型：`pg` 或 `milvus`
- `rag.rate-limit.global` — 全局并发限流
- `rag.memory` — 对话记忆配置
- `ai.providers` — AI 供应商配置
- `ai.chat.candidates` — 模型候选列表（按 priority 排序）
- `ai.embedding.candidates` — Embedding 模型候选
- `ai.rerank.candidates` — Rerank 模型候选
- `storage.s3` — 对象存储 (S3 兼容)

### 数据库
执行 `resources/database/schema_pg.sql` 初始化 PostgreSQL 表结构。
要求启用 `pgvector` 扩展。

### Docker 环境
参考 `resources/docker/lightweight/` 中的 compose 文件启动 Milvus 等依赖服务。

## 快速开始

1. 启动依赖服务（PostgreSQL, Redis, Milvus, RocketMQ）
2. 执行数据库初始化脚本
3. 配置 `application.yaml` 中的供应商 API Key
4. 启动 `bootstrap` 模块的 `BayMDApplication`
5. （可选）启动 `mcp-server` 模块的 `McpServerApplication`
6. 通过 API 创建知识库并上传文档
