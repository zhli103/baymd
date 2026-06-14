# BayMD 项目代码说明书

> 覆盖全部 4 个模块（bootstrap / framework / infra-ai / mcp-server）共 447 个 Java 文件，按包名分组，逐文件说明职责与关键逻辑。

---

## 目录

### 第一部分：bootstrap 应用层（359类）

- [1. 启动类](#1-启动类)
- [2. admin/ — 后台管理](#2-admin---后台管理)
- [3. core/ — 通用组件（分块+解析）](#3-core---通用组件)
  - [3.1 core/chunk/ — 分块策略](#31-corechunk---分块策略)
  - [3.2 core/parser/ — 文档解析](#32-coreparser---文档解析)
- [4. ingestion/ — 文档摄取（64类）](#4-ingestion---文档摄取)
  - [4.1 摄取API](#41-ingestioncontroller---摄取api)
  - [4.2 数据层](#42-ingestiondao---数据层)
  - [4.3 领域模型](#43-ingestiondomain---领域模型)
  - [4.4 摄取引擎](#44-ingestionengine---摄取引擎)
  - [4.5 摄取节点（6阶段）](#45-ingestionnode---摄取节点)
  - [4.6 业务服务](#46-ingestionservice---业务服务)
  - [4.7 获取策略](#47-ingestionstrategy---获取策略)
  - [4.8 Prompt + 工具类](#48-ingestionprompt--util---辅助)
- [5. knowledge/ — 知识库管理（61类）](#5-knowledge---知识库管理)
  - [5.1 知识库API](#51-knowledgecontroller---知识库api)
  - [5.2 数据层](#52-knowledgedao---数据层)
  - [5.3 配置与枚举](#53-knowledgeconfig--enums---配置与枚举)
  - [5.4 业务服务](#54-knowledgeservice---业务服务)
  - [5.5 消息队列](#55-knowledgemq---消息队列)
  - [5.6 定时调度](#56-knowledgeschedule---定时调度)
  - [5.7 过滤器与处理器](#57-knowledgefilter--handler---过滤器与处理器)
- [6. rag/ — RAG核心 ★★★（186类）](#6-rag---rag核心最重要)
  - [6.1 问答API入口](#61-ragcontroller---问答api入口)
  - [6.2 7阶段流式管线 ★★★](#62-ragservicepipeline---7阶段流式管线)
  - [6.3 SSE事件处理](#63-ragservicehandler---sse事件处理)
  - [6.4 分布式限流](#64-ragserviceratelimit---分布式限流)
  - [6.5 业务实现](#65-ragserviceimpl---业务实现)
  - [6.6 意图分类 ★★](#66-ragcoreintent---意图分类)
  - [6.7 知识库检索 ★★](#67-ragcoreretrieve---知识库检索)
  - [6.8 Prompt管理](#68-ragcoreprompt---prompt管理)
  - [6.9 对话记忆](#69-ragcorememory---对话记忆)
  - [6.10 Query改写](#610-ragcorerewrite---query改写)
  - [6.11 歧义引导](#611-ragcoreguidance---歧义引导)
  - [6.12 MCP工具集成](#612-ragcoremcp---mcp工具集成)
  - [6.13 向量存储](#613-ragcorevector---向量存储)
  - [6.14 检索通道](#614-ragcoreretrievechannels---检索通道)
  - [6.15 配置类](#615-ragconfig---配置类)
  - [6.16 横切关注点（AOP/Trace/MQ）](#616-ragaop--trace--mq---横切关注点)
  - [6.17 数据层](#617-ragdao---数据层)
  - [6.18 模型与工具（DTO/枚举/常量）](#618-ragdto--enums--constant--util---模型与工具)
- [7. user/ — 用户管理（20类）](#7-user---用户管理)

### 第二部分：基础设施模块（88类）

- [8. framework/ — 基础设施层（40类）](#8-framework--基础设施层40类)
  - [8.1 自动配置](#81-config--自动配置)
  - [8.2 上下文传递](#82-context--上下文传递)
  - [8.3 通用模型](#83-convention--通用模型)
  - [8.4 Web 层工具](#84-web--web-层工具)
  - [8.5 异常体系](#85-exception--异常体系)
  - [8.6 错误码](#86-errorcode--错误码)
  - [8.7 幂等性](#87-idempotent--幂等性)
  - [8.8 消息队列](#88-mq--消息队列)
  - [8.9 链路追踪](#89-trace--链路追踪)
  - [8.10 分布式ID](#810-distributedid--分布式id)
  - [8.11 缓存与数据库](#811-cache--database--缓存与数据库)
- [9. infra-ai/ — AI 基础设施层 ★★★（45类）](#9-infra-ai--ai-基础设施层45类)
  - [9.1 chat/ — 对话客户端](#91-chat--对话客户端)
  - [9.2 embedding/ — 向量化服务](#92-embedding--向量化服务)
  - [9.3 rerank/ — 重排序服务](#93-rerank--重排序服务)
  - [9.4 model/ — 模型路由与健康检查 ★★](#94-model--模型路由与健康检查-)
  - [9.5 token/ — Token 计数](#95-token--token-计数)
  - [9.6 http/ — HTTP 工具](#96-http--http-工具)
  - [9.7 config/ + enums/ + util/](#97-config--enums--util--配置与工具)
- [10. mcp-server/ — MCP工具服务器（3类）](#10-mcp-server--mcp工具服务器3类)

### 附录

- [附录A：设计模式应用索引](#附录a设计模式应用索引)
- [附录B：请求处理完整调用链](#附录b请求处理完整调用链)
- [附录C：关键配置速查](#附录c关键配置速查)
- [附录D：全部模块依赖关系](#附录d全部模块依赖关系)
- [附录E：infra-ai 完整调用链](#附录einfra-ai-完整调用链)
- [附录F：全项目配置速查](#附录f关键配置速查)

---

## 1. 启动类

| 文件 | 说明 |
|------|------|
| `BayMDApplication.java` | Spring Boot 启动入口，`@SpringBootApplication` 标注 |

## 2. admin/ — 后台管理

仪表盘统计数据，独立功能模块。

| 文件 | 说明 |
|------|------|
| `controller/DashboardController.java` | `/dashboard/*` API，提供运营数据概览和性能趋势 |
| `controller/vo/DashboardOverviewVO.java` | 总览数据视图：用户数、会话数、问答次数等 |
| `controller/vo/DashboardOverviewKpiVO.java` | 单个 KPI 指标（名称+数值+环比变化） |
| `controller/vo/DashboardOverviewGroupVO.java` | KPI 分组视图 |
| `controller/vo/DashboardPerformanceVO.java` | 性能统计：平均响应时间、检索耗时等 |
| `controller/vo/DashboardTrendPointVO.java` | 趋势图的单个数据点（时间+值） |
| `controller/vo/DashboardTrendSeriesVO.java` | 趋势图的一个系列（多条数据线） |
| `controller/vo/DashboardTrendsVO.java` | 趋势图总览：多个系列的时间序列数据 |
| `service/DashboardService.java` | 仪表盘服务接口 |
| `service/impl/DashboardServiceImpl.java` | 仪表盘服务实现，SQL 聚合查询统计 |

## 3. core/ — 通用组件

与业务无关的底层组件：分块、解析。

### 3.1 core/chunk/ — 分块策略

文档上传后被切成小块（Chunk），每块独立向量化。策略模式实现。

| 文件 | 说明 |
|------|------|
| `ChunkingStrategy.java` | **接口**：定义 `chunk(text, options) → List<VectorChunk>` |
| `ChunkingMode.java` | **枚举**：`FIXED_SIZE`（固定大小）和 `STRUCTURE_AWARE`（语义感知）。每个枚举值实现 `createOptions()` 方法构建对应配置 |
| `ChunkingOptions.java` | **接口**：分块参数（块大小、重叠大小），不同策略有不同实现 |
| `FixedSizeOptions.java` | 固定大小分块的参数：`chunkSize` + `overlapSize` |
| `TextBoundaryOptions.java` | 语义分块的参数：`targetChars`、`overlapChars`、`maxChars`、`minChars` |
| `ChunkingStrategyFactory.java` | **工厂**：根据 `ChunkingMode` 选择对应的 `ChunkingStrategy` 实现 |
| `ChunkEmbeddingService.java` | 对 `VectorChunk` 列表批量调用 Embedding API 生成向量 |
| `VectorChunk.java` | 模型类：`chunkId` + `index` + `content` + `embedding`（向量） |
| `strategy/FixedSizeTextChunker.java` | 固定大小分块实现：按字符数切割，支持重叠 |
| `strategy/StructureAwareTextChunker.java` | 语义分块实现：优先在段落/标题边界切分，保留 Markdown 结构 |

### 3.2 core/parser/ — 文档解析

将各种格式的文档提取为纯文本。策略模式实现。

| 文件 | 说明 |
|------|------|
| `DocumentParser.java` | **接口**：定义 `extractText(inputStream, filename) → String` |
| `ParserType.java` | **枚举**：`TIKA`（通用）和 `MARKDOWN` |
| `DocumentParserSelector.java` | 根据文件类型/`ParserType` 选择合适的 `DocumentParser` |
| `ParseResult.java` | 解析结果：文本内容 + 元数据 |
| `TextCleanupUtil.java` | 文本清洗工具：去除无效空白、控制字符 |
| `TikaDocumentParser.java` | 基于 Apache Tika，支持 PDF/DOC/DOCX/HTML/PPT 等格式 |
| `MarkdownDocumentParser.java` | 纯 Markdown 文件解析，保留标题结构 |


## 4. ingestion/ — 文档摄取

文档从来源到入库的完整流水线：**Fetcher → Parser → Enhancer → Chunker → Enricher → Indexer**。

### 4.1 ingestion/controller/ — 摄取API

| 文件 | 说明 |
|------|------|
| `IngestionPipelineController.java` | 摄取流水线的 CRUD API |
| `IngestionTaskController.java` | 摄取任务的创建、查询、重试 API |
| `request/IngestionPipelineCreateRequest.java` | 创建流水线请求体 |
| `request/IngestionPipelineNodeRequest.java` | 流水线节点配置请求体 |
| `request/IngestionPipelineUpdateRequest.java` | 更新流水线请求体 |
| `request/IngestionTaskCreateRequest.java` | 创建摄取任务请求体 |
| `vo/IngestionPipelineVO.java` | 流水线详情视图 |
| `vo/IngestionPipelineNodeVO.java` | 流水线节点视图 |
| `vo/IngestionTaskVO.java` | 摄取任务视图 |
| `vo/IngestionTaskNodeVO.java` | 任务节点执行状态视图 |

### 4.2 ingestion/dao/ — 数据层

| 文件 | 说明 |
|------|------|
| `entity/IngestionPipelineDO.java` | 流水线数据库实体 |
| `entity/IngestionPipelineNodeDO.java` | 流水线节点实体（含 settings_json、condition_json） |
| `entity/IngestionTaskDO.java` | 摄取任务实体（状态、进度） |
| `entity/IngestionTaskNodeDO.java` | 任务节点执行记录（状态、耗时、错误信息） |
| `mapper/IngestionPipelineMapper.java` | 流水线 MyBatis Mapper |
| `mapper/IngestionPipelineNodeMapper.java` | 流水线节点 Mapper |
| `mapper/IngestionTaskMapper.java` | 任务 Mapper |
| `mapper/IngestionTaskNodeMapper.java` | 任务节点 Mapper |

### 4.3 ingestion/domain/ — 领域模型

| 文件 | 说明 |
|------|------|
| `context/IngestionContext.java` | 摄取上下文：在整个流水线节点间传递的共享状态 |
| `context/DocumentSource.java` | 文档来源（URL/本地文件/S3） |
| `context/StructuredDocument.java` | 结构化文档：解析后的文本 + 元数据 + 分块列表 |
| `context/NodeLog.java` | 节点执行日志 |
| `enums/IngestionNodeType.java` | 节点类型枚举：FETCHER/PARSER/ENHANCER/CHUNKER/ENRICHER/INDEXER |
| `enums/IngestionStatus.java` | 任务状态：PENDING/RUNNING/SUCCESS/FAILED |
| `enums/SourceType.java` | 文档来源类型：FILE/URL |
| `enums/EnhanceType.java` | 增强类型 |
| `enums/ChunkEnrichType.java` | 分块增强类型 |
| `pipeline/PipelineDefinition.java` | 流水线定义：节点列表 + 执行顺序 |
| `pipeline/NodeConfig.java` | 节点配置：节点类型 + settings + 条件 |
| `result/IngestionResult.java` | 摄取结果：最终输出的分块列表 |
| `result/NodeResult.java` | 单个节点的执行结果 |
| `settings/ChunkerSettings.java` | 分块节点配置参数 |
| `settings/EnhancerSettings.java` | 增强节点配置参数 |
| `settings/EnricherSettings.java` | 富化节点配置参数 |
| `settings/IndexerSettings.java` | 索引节点配置参数 |
| `settings/ParserSettings.java` | 解析节点配置参数 |

### 4.4 ingestion/engine/ — 摄取引擎

| 文件 | 说明 |
|------|------|
| `IngestionEngine.java` | **核心**：按流水线定义顺序执行节点，支持条件判断和错误处理 |
| `ConditionEvaluator.java` | 条件求值器：根据 JSON 表达式判断是否跳过某个节点 |
| `NodeOutputExtractor.java` | 节点输出提取器：从 `IngestionContext` 中提取上一个节点的产出 |

### 4.5 ingestion/node/ — 摄取节点

6 个节点对应 6 个阶段，每个节点实现 `IngestionNode` 接口。

| 文件 | 说明 |
|------|------|
| `IngestionNode.java` | **接口**：`execute(context) → NodeResult` |
| `FetcherNode.java` | 获取阶段：从 URL/文件/S3 拉取原始文档 |
| `ParserNode.java` | 解析阶段：调用 `DocumentParserSelector` 将原始文档转为纯文本 |
| `EnhancerNode.java` | 增强阶段：调用 LLM 对文本做摘要/关键词提取等增强 |
| `ChunkerNode.java` | 分块阶段：调用 `ChunkingStrategy` 将文本切分为多个 Chunk |
| `EnricherNode.java` | 富化阶段：调用 LLM 对每个 Chunk 添加上下文信息 |
| `IndexerNode.java` | 索引阶段：调用 Embedding 服务生成向量并写入向量库 |

### 4.6 ingestion/service/ — 业务服务

| 文件 | 说明 |
|------|------|
| `IngestionPipelineService.java` | 流水线管理接口 |
| `impl/IngestionPipelineServiceImpl.java` | 流水线 CRUD 实现 |
| `IngestionTaskService.java` | 摄取任务接口 |
| `impl/IngestionTaskServiceImpl.java` | 任务创建、执行、重试实现 |
| `IntentTreeService.java` | 意图树管理接口（含 `initFromFactory()`） |
| `impl/IntentTreeServiceImpl.java` | **重要**：意图树 CRUD + 从工厂初始化。`initFromFactory()` 在启动时由 `MedicalIntentTreeInitializer` 调用 |

### 4.7 ingestion/strategy/fetcher/ — 文档获取策略

策略模式实现，不同数据源用不同获取器。

| 文件 | 说明 |
|------|------|
| `DocumentFetcher.java` | **接口**：`fetch(source) → FetchResult` |
| `FetchResult.java` | 获取结果：InputStream + 文件名 + 大小 |
| `HttpUrlFetcher.java` | HTTP/HTTPS URL 下载 |
| `LocalFileFetcher.java` | 本地文件系统读取 |
| `S3Fetcher.java` | S3 对象存储读取 |
| `FeishuFetcher.java` | 飞书文档获取 |

### 4.8 ingestion/prompt/ + util/ — 辅助

| 文件 | 说明 |
|------|------|
| `prompt/EnhancerPromptManager.java` | Enhancer 节点的 Prompt 模板管理 |
| `prompt/EnricherPromptManager.java` | Enricher 节点的 Prompt 模板管理 |
| `util/HttpClientHelper.java` | HTTP 客户端工具：带超时和重试的请求封装 |
| `util/JsonResponseParser.java` | JSON 响应解析工具 |
| `util/MimeTypeDetector.java` | MIME 类型检测 |
| `util/PromptTemplateRenderer.java` | Prompt 模板渲染工具 |

## 5. knowledge/ — 知识库管理

知识库和文档的增删改查，文档上传、分块、索引的全流程。

### 5.1 knowledge/controller/ — 知识库API

| 文件 | 说明 |
|------|------|
| `KnowledgeBaseController.java` | 知识库 CRUD：创建(`POST`)、删除(`DELETE`)、查询(`GET`)、分页列表 |
| `KnowledgeDocumentController.java` | **重要**：文档上传(`POST /{kbId}/docs/upload`)、分块触发(`POST /docs/{id}/chunk`)、启用禁用 |
| `KnowledgeChunkController.java` | 分块查询、更新、批量操作 API |
| `request/KnowledgeBaseCreateRequest.java` | 创建知识库请求：`name` + `embeddingModel` + `collectionName` |
| `request/KnowledgeBasePageRequest.java` | 知识库分页查询请求 |
| `request/KnowledgeBaseUpdateRequest.java` | 更新知识库请求 |
| `request/KnowledgeDocumentCreateRequest.java` | 创建文档请求 |
| `request/KnowledgeDocumentUploadRequest.java` | 上传文档请求：`sourceType` + `processMode` + `chunkStrategy` |
| `request/KnowledgeDocumentPageRequest.java` | 文档分页查询请求 |
| `request/KnowledgeDocumentUpdateRequest.java` | 更新文档请求 |
| `request/KnowledgeChunkCreateRequest.java` | 创建分块请求：`chunkId` + `index` + `content` |
| `request/KnowledgeChunkPageRequest.java` | 分块分页查询请求 |
| `request/KnowledgeChunkUpdateRequest.java` | 更新分块请求 |
| `request/KnowledgeChunkBatchRequest.java` | 批量分块操作请求 |
| `vo/KnowledgeBaseVO.java` | 知识库视图（含 `documentCount`） |
| `vo/KnowledgeDocumentVO.java` | 文档视图（含状态、分块数等） |
| `vo/KnowledgeDocumentSearchVO.java` | 文档搜索结果视图 |
| `vo/KnowledgeDocumentChunkLogVO.java` | 分块日志视图（各阶段耗时） |
| `vo/KnowledgeChunkVO.java` | 分块内容视图 |
| `vo/ChunkStrategyVO.java` | 分块策略选项视图（`value` + `label` + `defaultConfig`） |

### 5.2 knowledge/dao/ — 数据层

| 文件 | 说明 |
|------|------|
| `entity/KnowledgeBaseDO.java` | 知识库实体：`id`、`name`、`embeddingModel`、`collectionName` |
| `entity/KnowledgeDocumentDO.java` | 文档实体：`kbId`、`docName`、`fileUrl`、`status`、`chunkCount` |
| `entity/KnowledgeChunkDO.java` | 分块实体：`docId`、`chunkIndex`、`content`、`contentHash` |
| `entity/KnowledgeDocumentChunkLogDO.java` | 分块执行日志：各阶段耗时（extract/chunk/embed/persist） |
| `entity/KnowledgeDocumentScheduleDO.java` | 文档定时刷新配置：`cronExpr`、`lastEtag`、`lockUntil` |
| `entity/KnowledgeDocumentScheduleExecDO.java` | 定时刷新执行记录 |
| `handler/JsonbTypeHandler.java` | MyBatis JSONB 类型处理器（PostgreSQL jsonb 字段与 Java Map 互转） |
| `mapper/KnowledgeBaseMapper.java` | 知识库 Mapper |
| `mapper/KnowledgeDocumentMapper.java` | 文档 Mapper |
| `mapper/KnowledgeChunkMapper.java` | 分块 Mapper |
| `mapper/KnowledgeDocumentChunkLogMapper.java` | 分块日志 Mapper |
| `mapper/KnowledgeDocumentScheduleMapper.java` | 文档定时任务 Mapper |
| `mapper/KnowledgeDocumentScheduleExecMapper.java` | 定时任务执行记录 Mapper |

### 5.3 knowledge/config/ + enums/ — 配置与枚举

| 文件 | 说明 |
|------|------|
| `config/KnowledgeScheduleProperties.java` | 定时刷新配置属性：扫描间隔、锁时长、批次大小 |
| `config/RagSemaphoreProperties.java` | 上传限流信号量配置：名称、最大并发、等待时间 |
| `config/SemaphoreInitializer.java` | 启动时初始化 Redisson 信号量（确保信号量 key 存在） |
| `enums/DocumentStatus.java` | 文档状态：`PENDING`/`RUNNING`/`SUCCESS`/`FAILED` |
| `enums/ProcessMode.java` | 处理模式：`CHUNK`(直接分块) / `PIPELINE`(走摄取流水线) |
| `enums/SourceType.java` | 文档来源类型：`FILE`(上传文件) / `URL`(远程地址) |
| `enums/ScheduleRunStatus.java` | 定时任务执行状态：`SUCCESS`/`FAILED` |

### 5.4 knowledge/service/ — 业务服务

| 文件 | 说明 |
|------|------|
| `KnowledgeBaseService.java` | 知识库业务接口 |
| `impl/KnowledgeBaseServiceImpl.java` | **重要**：创建KB时同时创建 S3 bucket 和 Milvus/PG 向量空间 |
| `KnowledgeDocumentService.java` | 文档业务接口 |
| `impl/KnowledgeDocumentServiceImpl.java` | **核心**：文档上传→存储→分块→向量化→索引的全流程。约800行最复杂的Service |
| `KnowledgeChunkService.java` | 分块业务接口 |
| `impl/KnowledgeChunkServiceImpl.java` | 分块 CRUD 实现 |
| `KnowledgeDocumentScheduleService.java` | 文档定时刷新接口 |
| `impl/KnowledgeDocumentScheduleServiceImpl.java` | 定时刷新实现：检测远程文件变更，触发重新摄取 |

### 5.5 knowledge/mq/ — 消息队列

文档分块通过 RocketMQ 异步执行，解耦上传和分块。

| 文件 | 说明 |
|------|------|
| `event/KnowledgeDocumentChunkEvent.java` | 文档分块事件：`docId` + `operator` |
| `KnowledgeDocumentChunkConsumer.java` | **消费者**：收到事件后调用 `executeChunk()` 执行分块 |
| `KnowledgeDocumentChunkTransactionChecker.java` | **事务回查器**：检查半消息对应的文档是否真的需要分块（防丢失） |

### 5.6 knowledge/schedule/ — 定时调度

定时检测远程文档是否更新，自动重新摄取。

| 文件 | 说明 |
|------|------|
| `KnowledgeDocumentScheduleJob.java` | 定时任务入口：扫描待执行的定时刷新任务 |
| `ScheduleRefreshProcessor.java` | 刷新处理器：比较 ETag/LastModified/ContentHash，有变化则重新摄取 |
| `ScheduleLockManager.java` | 分布式锁管理：防止多实例同时刷新同一文档 |
| `ScheduleLockLease.java` | 锁租约对象 |
| `ScheduleStateManager.java` | 任务状态管理器：记录刷新历史和下一执行时间 |
| `ScheduleStateContext.java` | 状态上下文 |
| `CronScheduleHelper.java` | Cron 表达式计算工具 |
| `DocumentStatusHelper.java` | 文档状态辅助工具 |

### 5.7 knowledge/filter/ + handler/ — 过滤器与处理器

| 文件 | 说明 |
|------|------|
| `filter/UploadRateLimitFilter.java` | **上传限流过滤器**：基于 Redisson 信号量，在文件上传前获取许可，超限返回429 |
| `handler/RemoteFileFetcher.java` | 远程文件获取处理器：下载URL文件并存储到本地/S3 |

## 6. rag/ — RAG核心（最重要）

整个问答系统的"大脑"。从用户输入到流式回答的完整处理链都在这里。

### 6.1 rag/controller/ — 问答API入口

| 文件 | 说明 |
|------|------|
| `RAGChatController.java` | **问答入口**：`GET /rag/v3/chat?question=xxx`。创建 SSE 连接，委托 `RAGChatService.streamChat()` 处理。含幂等防重 |
| `ConversationController.java` | 会话管理：创建、列表查询、更新标题、删除 |
| `IntentTreeController.java` | 意图树管理 API：查询树、增删改节点、批量启用/禁用/删除 |
| `MessageFeedbackController.java` | 消息反馈：点赞/踩 |
| `RAGSettingsController.java` | RAG 配置查询 API：检索通道参数、分块策略列表等 |
| `RagTraceController.java` | Trace 链路查询：查看某次问答的完整调用链 |
| `QueryTermMappingController.java` | 关键词归一化映射管理 |
| `SampleQuestionController.java` | 预置示例问题管理 |
| `request/ConversationCreateRequest.java` | 创建会话请求 |
| `request/ConversationUpdateRequest.java` | 更新会话请求 |
| `request/DocumentSourceRequest.java` | 文档来源请求 |
| `request/IntentNodeCreateRequest.java` | 创建意图节点请求 |
| `request/IntentNodeUpdateRequest.java` | 更新意图节点请求 |
| `request/IntentNodeBatchRequest.java` | 批量意图节点操作请求 |
| `request/MessageFeedbackRequest.java` | 反馈请求（vote + reason） |
| `request/QueryTermMappingCreateRequest.java` | 创建词映射请求 |
| `request/QueryTermMappingUpdateRequest.java` | 更新词映射请求 |
| `request/QueryTermMappingPageRequest.java` | 分页查询词映射请求 |
| `request/SampleQuestionCreateRequest.java` | 创建示例问题请求 |
| `request/SampleQuestionUpdateRequest.java` | 更新示例问题请求 |
| `request/SampleQuestionPageRequest.java` | 分页查询示例问题请求 |
| `request/RagTraceRunPageRequest.java` | 分页查询Trace请求 |
| `vo/ConversationVO.java` | 会话视图 |
| `vo/ConversationMessageVO.java` | 消息视图（含 thinking 内容） |
| `vo/IntentNodeTreeVO.java` | 意图节点树视图（含 children 递归结构） |
| `vo/QueryTermMappingVO.java` | 词映射视图 |

*(controller/vo/ 下还有更多 VO 类，均为接口返回数据结构，不再逐一列出)*

### 6.2 rag/service/pipeline/ — 7阶段流式管线 ★★★

**这是整个项目最核心的代码。** 承载一次问答从输入到输出的完整流程。

| 文件 | 说明 |
|------|------|
| `StreamChatPipeline.java` | **★★★ 核心管线**：7阶段方法链——`loadMemory`→`rewriteQuery`→`resolveIntents`→`handleGuidance`→`handleSystemOnly`→`retrieve`→`streamRagResponse`。每个阶段是一个私有方法，返回 boolean 表示是否短路 |
| `StreamChatContext.java` | 管线上下文对象：在7个阶段间传递 question、history、rewriteResult、subIntents、callback 等 |

**7 阶段详解：**

| 阶段 | 方法 | 功能 |
|------|------|------|
| 1 | `loadMemory(ctx)` | 从数据库加载最近 N 轮对话历史（短时记忆） |
| 2 | `rewriteQuery(ctx)` | 调用 LLM 改写用户问题（去除口语化、补全指代）、判断是否拆分 |
| 3 | `resolveIntents(ctx)` | 对每个子问题并行进行意图分类，匹配意图树节点 |
| 4 | `handleGuidance(ctx)` | 检测是否存在品类歧义，生成引导选项让用户澄清 |
| 5 | `handleSystemOnly(ctx)` | 若所有意图均为 SYSTEM 类型（如问候），直接 LLM 回复，跳过检索 |
| 6 | `retrieve(ctx)` | 多通道检索知识库 + MCP 工具调用，返回 KB 上下文和 MCP 数据 |
| 7 | `streamRagResponse(ctx, retrievalCtx)` | 组装 Prompt → 调用 LLM 流式生成 → SSE 推送给客户端 |

### 6.3 rag/service/handler/ — SSE事件处理

将 LLM 输出的每个 Token 封装为 SSE 事件发送给客户端。

| 文件 | 说明 |
|------|------|
| `StreamChatEventHandler.java` | **SSE 事件处理器**：实现 `StreamCallback`，`onContent(chunk)` 把 token 封装为 `MessageDelta` SSE 事件；`onComplete()` 持久化消息并发送 `[DONE]`；`onError()` 发送错误事件 |
| `StreamChatHandlerParams.java` | 构造参数对象：emitter、conversationId、taskId、memoryService 等 |
| `StreamCallbackFactory.java` | 工厂：创建 `StreamChatEventHandler` 实例 |
| `StreamTaskManager.java` | 任务管理器：注册/取消 SSE 任务，支持客户端中途取消生成 |

### 6.4 rag/service/ratelimit/ — 分布式限流

| 文件 | 说明 |
|------|------|
| `FairDistributedRateLimiter.java` | **分布式公平队列限流器**：基于 Redis ZSET + Lua 原子脚本 |
| `ChatQueueLimiter.java` | 聊天队列限流：将请求排队，按顺序获取许可后才执行管线。超限时返回排队提示 |
| `RateLimitEvent.java` | 限流事件实体 |
| `RejectedContext.java` | 拒绝上下文（排队超限时使用） |

### 6.5 rag/service/impl/ — 业务实现

| 文件 | 说明 |
|------|------|
| `RAGChatServiceImpl.java` | **问答服务入口**：限流检查→创建 SSE 回调→启动 Trace→执行管线 |
| `ConversationServiceImpl.java` | 会话服务：创建、列表查询、消息分页 |
| `ConversationMessageServiceImpl.java` | 消息服务：消息持久化、分页查询 |
| `ConversationGroupServiceImpl.java` | 会话分组服务 |
| `MessageFeedbackServiceImpl.java` | 反馈服务：点赞/踩事件通过 RocketMQ 异步处理 |
| `SampleQuestionServiceImpl.java` | 示例问题管理实现 |
| `QueryTermMappingAdminServiceImpl.java` | 词映射管理实现 |
| `RagTraceQueryServiceImpl.java` | Trace 查询服务 |
| `RagTraceRecordServiceImpl.java` | Trace 记录服务 |
| `S3FileStorageService.java` | S3 文件存储实现：预签名URL流式上传、getObject下载 |
| `LocalFileStorageService.java` | 本地文件存储实现：开发环境使用，文件存本地磁盘 |

### 6.6 rag/core/intent/ — 意图分类 ★★

将用户问题路由到正确的知识分类。树形三级结构（DOMAIN→CATEGORY→TOPIC），LLM 打分匹配。

| 文件 | 说明 |
|------|------|
| `IntentNode.java` | **意图节点模型**：`id`、`name`、`level`、`kind`(KB/SYSTEM/MCP)、`kbId`、`mcpToolId`、`description`、`examples`、`promptTemplate`、`children` |
| `IntentTreeFactory.java` | **意图树工厂**：硬编码构建医学意图树（科室推荐/症状自查/药物查询/饮食建议/中医辨证/报告解读/医院推荐 + 系统交互） |
| `DefaultIntentClassifier.java` | **LLM 意图分类器**：列出所有叶子节点给 LLM，要求 LLM 对每个节点打分（0-1）。从 Redis 缓存加 载意图树，调用 LLM 做分类，解析 JSON 结果 |
| `IntentResolver.java` | 意图解析器：调用分类器→按阈值过滤→限制数量→合并多意图结果 |
| `IntentClassifier.java` | 意图分类器接口 |
| `IntentTreeCacheManager.java` | 意图树 Redis 缓存管理器：序列化/反序列化意图树 |
| `IntentNodeRegistry.java` | 意图节点注册表接口：按 ID 查找节点 |
| `NodeScore.java` | 意图匹配分数：`IntentNode` + `score`（0-1） |
| `NodeScoreFilters.java` | 节点分数过滤器：按 kind 过滤（`.kb()`/`.mcp()`/`.system()`） |

### 6.7 rag/core/retrieve/ — 知识库检索 ★★

| 文件 | 说明 |
|------|------|
| `RetrievalEngine.java` | **检索引擎入口**：根据意图列表，并行执行 KB 检索 + MCP 工具调用，整合结果 |
| `MultiChannelRetrievalEngine.java` | **多通道检索引擎**：并行执行多个检索通道→去重→重排序→返回最终 Chunk 列表 |
| `RetrieverService.java` | 检索服务接口（抽象了 pgvector 和 Milvus） |
| `PgRetrieverService.java` | PostgreSQL pgvector 检索实现：用向量相似度查询 `t_knowledge_vector` |
| `MilvusRetrieverService.java` | Milvus 向量检索实现 |
| `VectorStoreService.java` | 向量存储接口：写入、删除、搜索 |
| `PgVectorStoreService.java` | PG 向量存储实现 |
| `MilvusVectorStoreService.java` | Milvus 向量存储实现 |
| `VectorStoreAdmin.java` | 向量存储管理：创建/删除向量空间 |
| `VectorSpaceId.java` | 向量空间标识：`logicalName` |
| `VectorSpaceSpec.java` | 向量空间规格：空间ID + 维度 + 距离度量 |

### 6.8 rag/core/prompt/ — Prompt管理

| 文件 | 说明 |
|------|------|
| `RAGPromptService.java` | **Prompt 组装服务**：根据场景（KB/MCP/Mixed）选择模板，组装 system + evidence + history + user 消息列表 |
| `PromptTemplateLoader.java` | **模板加载器**：从 classpath 加载 `.st` 模板文件，缓存，支持 `{slot}` 变量填充和 section 分段 |
| `PromptTemplateUtils.java` | 模板工具：占位符填充、Markdown 清理、Section 解析 |
| `PromptContext.java` | Prompt 上下文：`question`、`mcpContext`、`kbContext`、`mcpIntents`、`kbIntents` |
| `ContextFormatter.java` | 上下文格式化器：将检索结果和 MCP 数据格式化为 LLM 可读的 XML 标签格式 |
| `PromptScene.java` | Prompt 场景枚举：`KB_ONLY`/`MCP_ONLY`/`MIXED`/`EMPTY` |
| `PromptPlan.java` | Prompt 方案：意图列表 + 基础模板 |
| `PromptBuildPlan.java` | Prompt 构建方案：场景 + 模板 + 上下文 |

### 6.9 rag/core/memory/ — 对话记忆

| 文件 | 说明 |
|------|------|
| `ConversationMemoryService.java` | **记忆服务接口**：加载历史、添加消息 |
| `DefaultConversationMemoryService.java` | **实现**：从 MySQL 加载最近 N 轮对话；超 N 轮触发摘要生成压缩历史 |
| `ConversationMemorySummaryService.java` | 摘要服务接口 |
| `JdbcConversationMemorySummaryService.java` | 基于 JDBC 的摘要存储实现 |

### 6.10 rag/core/rewrite/ — Query改写

| 文件 | 说明 |
|------|------|
| `QueryRewriteService.java` | 改写服务接口 |
| `MultiQuestionRewriteService.java` | **实现**：调用 LLM 改写用户问题（去口语化、补全指代），并判断是否需拆分为多个子问题并行处理 |
| `RewriteResult.java` | 改写结果：`rewrittenQuestion` + `shouldSplit` + `subQuestions` |
| `QueryTermMappingService.java` | 关键词归一化映射：将同义词映射到标准词（如"退款"→"退货"） |

### 6.11 rag/core/guidance/ — 歧义引导

当问题模糊到可能匹配多个意图时，生成引导选项让用户选择。

| 文件 | 说明 |
|------|------|
| `IntentGuidanceService.java` | **歧义检测服务**：调用 LLM 判断是否存在品类歧义，生成引导 Prompt |
| `GuidanceDecision.java` | 引导决策结果：`ambiguous` + `prompt` |

### 6.12 rag/core/mcp/ — MCP工具集成

Model Context Protocol 工具注册、参数提取、执行。

| 文件 | 说明 |
|------|------|
| `McpToolRegistry.java` | MCP 工具注册表：按 toolId 管理所有注册的 MCP 工具执行器 |
| `DefaultMcpToolRegistry.java` | 默认实现：启动时扫描所有 `SyncToolSpecification` Bean |
| `McpToolExecutor.java` | MCP 工具执行器接口：`getToolDefinition()` + `execute(params)` |
| `McpParameterExtractor.java` | **参数提取器**：调用 LLM 从用户问题中提取 MCP 工具所需的参数 |
| `McpClientAutoConfiguration.java` | MCP 客户端自动配置：连接 MCP Server，注册其提供的工具 |

### 6.13 rag/core/vector/ — 向量存储

| 文件 | 说明 |
|------|------|
| `VectorStoreService.java` | 向量存储服务接口：`indexDocumentChunks`、`search`、`deleteDocumentVectors` |
| `PgVectorStoreService.java` | PostgreSQL pgvector 实现：用 HNSW 索引做余弦相似度检索 |
| `MilvusVectorStoreService.java` | Milvus 实现 |
| `VectorStoreAdmin.java` | 向量空间管理接口：`ensureVectorSpace`（不存在则创建） |
| `VectorSpaceId.java` | 向量空间 ID |
| `VectorSpaceSpec.java` | 向量空间规格（维度、距离度量） |

### 6.14 rag/core/retrieve/channels/ — 检索通道

每个通道是一种检索策略，由 `MultiChannelRetrievalEngine` 并行执行。

| 文件 | 说明 |
|------|------|
| `SearchChannel.java` | **检索通道基类**：定义 `retrieve(subIntents, topK) → List<RetrievedChunk>` |
| `VectorGlobalSearchChannel.java` | **全局向量检索**：不区分意图，在整个向量空间中检索 |
| `IntentDirectedSearchChannel.java` | **意图定向检索**：根据意图节点指定的 KB/Collection 做定向检索 |
| `AbstractParallelRetriever.java` | 并行检索抽象基类：封装多意图并行检索的通用逻辑 |
| `RetrieverSelector.java` | 检索器选择器：根据配置（pg/milvus）选择对应的 Retriever 实现 |
| `postprocessor/DeduplicationPostProcessor.java` | 后处理器：多通道结果按内容去重 |
| `postprocessor/RerankPostProcessor.java` | 后处理器：调用 Rerank 模型对 Chunk 重新打分排序 |

### 6.15 rag/config/ — 配置类

| 文件 | 说明 |
|------|------|
| `MedicalIntentTreeInitializer.java` | **启动初始化器**：`CommandLineRunner`，应用启动时调用 `IntentTreeService.initFromFactory()` 初始化意图树 |
| `RAGConfigProperties.java` | RAG 通用配置属性类 |
| `RAGDefaultProperties.java` | RAG 默认配置（SSE 超时、维度、度量类型等） |
| `RAGRateLimitProperties.java` | 限流配置属性 |
| `SearchChannelProperties.java` | 检索通道配置（置信度阈值、TopK 倍数等） |
| `MemoryProperties.java` | 记忆配置（保留轮数、摘要阈值等） |
| `GuidanceProperties.java` | 歧义引导配置 |
| `RagTraceProperties.java` | Trace 配置 |
| `DemoModeProperties.java` | Demo 模式配置 |
| `DemoModeInterceptor.java` | Demo 模式拦截器：`demo-mode=true` 时拦截写操作 |
| `ChatRateLimiterConfig.java` | 聊天限流器配置 |
| `HttpClientConfig.java` | OkHttpClient Bean 配置（同步/流式两种客户端） |
| `ThreadPoolExecutorConfig.java` | 线程池配置：意图分类线程池、检索线程池、MCP 线程池 |
| `MilvusConfig.java` | Milvus 客户端配置 |
| `RestFSS3Config.java` | S3 客户端配置（RustFS）：`@ConditionalOnProperty("rustfs.url")` |
| `WebConfig.java` | Web MVC 配置（跨域等） |
| `Utf8ResponseFilter.java` | UTF-8 响应过滤器 |
| `validation/MemoryConfigValidator.java` | 记忆配置校验器 |
| `validation/ValidMemoryConfig.java` | 记忆配置校验注解 |

### 6.16 rag/aop/ + trace/ + mq/ — 横切关注点

| 文件 | 说明 |
|------|------|
| `aop/RagTraceAspect.java` | **Trace 切面**：拦截 `@RagTraceNode` 注解的方法，记录执行耗时和状态 |
| `trace/StreamChatTraceRunner.java` | Trace 包装器：在 Trace 上下文中执行管线，记录完整调用链 |
| `mq/MessageFeedbackConsumer.java` | 消息反馈消费者：异步处理点赞/踩数据 |

### 6.17 rag/dao/ — 数据层

| 文件 | 说明 |
|------|------|
| `entity/ConversationDO.java` | 会话实体 |
| `entity/ConversationSummaryDO.java` | 会话摘要实体 |
| `entity/MessageDO.java` | 消息实体：`role`(user/assistant) + `content` + `thinkingContent` |
| `entity/MessageFeedbackDO.java` | 消息反馈实体 |
| `entity/IntentNodeDO.java` | 意图节点实体（对应 `t_intent_node` 表） |
| `entity/QueryTermMappingDO.java` | 词映射实体 |
| `entity/SampleQuestionDO.java` | 示例问题实体 |
| `entity/RagTraceRunDO.java` | Trace 运行记录实体 |
| `entity/RagTraceNodeDO.java` | Trace 节点记录实体 |
| `mapper/ConversationMapper.java` | 会话 Mapper |
| `mapper/ConversationSummaryMapper.java` | 摘要 Mapper |
| `mapper/MessageMapper.java` | 消息 Mapper |
| `mapper/MessageFeedbackMapper.java` | 反馈 Mapper |
| `mapper/IntentNodeMapper.java` | 意图节点 Mapper |
| `mapper/QueryTermMappingMapper.java` | 词映射 Mapper |
| `mapper/SampleQuestionMapper.java` | 示例问题 Mapper |
| `mapper/RagTraceRunMapper.java` | Trace 运行 Mapper |
| `mapper/RagTraceNodeMapper.java` | Trace 节点 Mapper |

### 6.18 rag/dto/ + enums/ + constant/ + util/ — 模型与工具

| 文件 | 说明 |
|------|------|
| `dto/MessageDelta.java` | SSE 消息增量：`{type: "response", content: "xxx"}` |
| `dto/MetaPayload.java` | SSE 元数据：`{conversationId, taskId}` |
| `dto/CompletionPayload.java` | SSE 完成载荷：`{messageId, title}` |
| `dto/RetrievalContext.java` | 检索上下文：`kbContext` + `mcpContext` + `intentChunks` |
| `dto/SubQuestionIntent.java` | 子问题-意图绑定：`subQuestion` + `nodeScores` |
| `dto/IntentGroup.java` | 意图分组：`mcpIntents` + `kbIntents` |
| `dto/IntentCandidate.java` | 意图候选（子问题索引 + 节点分数） |
| `dto/KbResult.java` | KB 检索结果：格式化上下文 + 按意图分组的 chunks |
| `dto/StoredFileDTO.java` | 存储文件信息：URL、类型、大小、原始文件名 |
| `enums/IntentKind.java` | 意图类型枚举：`KB`(0)/`SYSTEM`(1)/`MCP`(2) |
| `enums/IntentLevel.java` | 意图层级枚举：`DOMAIN`(0)/`CATEGORY`(1)/`TOPIC`(2) |
| `enums/SSEEventType.java` | SSE 事件类型：`META`/`MESSAGE`/`THINK`/`FINISH`/`DONE` |
| `enums/UserRole.java` | 用户角色枚举 |
| `constant/RAGConstant.java` | RAG 常量：意图分数阈值、最大意图数、各种 Prompt 模板路径 |
| `util/PromptSanitizer.java` | Prompt 安全处理工具 |

## 7. user/ — 用户管理

认证和用户管理（开发阶段已绕过登录校验）。

| 文件 | 说明 |
|------|------|
| `controller/AuthController.java` | 认证接口：`POST /auth/login`（登录）、`POST /auth/logout`（登出） |
| `controller/UserController.java` | 用户 CRUD 接口（需 admin 角色） |
| `service/AuthService.java` | 认证服务接口 |
| `impl/AuthServiceImpl.java` | 认证实现：用户名密码校验 → SaToken 登录 → 返回 token |
| `service/UserService.java` | 用户服务接口 |
| `impl/UserServiceImpl.java` | 用户 CRUD 实现 |
| `config/SaTokenConfig.java` | SaToken 拦截器配置：注册 SaInterceptor（开发阶段 `checkLogin` 已注释） |
| `config/UserContextInterceptor.java` | 用户上下文拦截器：从 SaToken 加载用户信息到 `UserContext` 线程变量。未登录时设置默认 system 用户 |
| `controller/request/LoginRequest.java` | 登录请求：`username` + `password` |
| `controller/request/UserCreateRequest.java` | 创建用户请求 |
| `controller/request/UserUpdateRequest.java` | 更新用户请求 |
| `controller/request/UserPageRequest.java` | 用户分页查询请求 |
| `controller/request/ChangePasswordRequest.java` | 修改密码请求 |
| `controller/vo/LoginVO.java` | 登录响应：`userId` + `role` + `token` + `avatar` |
| `controller/vo/CurrentUserVO.java` | 当前用户信息视图 |
| `controller/vo/UserVO.java` | 用户视图 |
| `dao/entity/UserDO.java` | 用户实体 |
| `dao/mapper/UserMapper.java` | 用户 Mapper |

---

## 附录A：设计模式应用索引

| 模式 | 应用位置 | 说明 |
|------|---------|------|
| **Chain of Responsibility** | `StreamChatPipeline` | 7阶段依次执行，每阶段可短路终止 |
| **Strategy** | `ChunkingStrategy`, `DocumentParser`, `DocumentFetcher` | 不同算法/格式用不同实现 |
| **Factory** | `ChunkingStrategyFactory`, `IntentTreeFactory`, `StreamCallbackFactory` | 根据配置创建合适实例 |
| **Template Method** | `AbstractOpenAIStyleChatClient`, `AbstractOpenAIStyleEmbeddingClient` | 定义骨架，子类覆写钩子方法 |
| **Registry** | `IntentNodeRegistry`, `McpToolRegistry` | 维护 ID→对象 的映射表 |
| **AOP** | `RagTraceAspect` | 切面记录方法耗时和异常 |
| **Circuit Breaker** | `ModelHealthStore`（infra-ai模块） | 三状态断路器保护下游AI服务 |
| **Observer** | RocketMQ 事件驱动 | 文档分块异步解耦 |

## 附录B：请求处理完整调用链

```
用户 curl "GET /rag/v3/chat?question=头疼"
  │
  ├─ RAGChatController.chat()
  │   └─ 创建 SseEmitter，提交给限流队列
  │
  ├─ RAGChatServiceImpl.streamChat()
  │   └─ StreamChatTraceRunner.run()  ← 建立 Trace 上下文
  │       └─ StreamChatPipeline.execute(ctx)
  │           │
  │           ├─ [1] loadMemory()          → 从DB加载历史对话
  │           ├─ [2] rewriteQuery()        → LLM改写问题
  │           ├─ [3] resolveIntents()      → LLM意图分类
  │           ├─ [4] handleGuidance()      → 歧义检测（可短路）
  │           ├─ [5] handleSystemOnly()    → 系统直答（可短路）
  │           ├─ [6] retrieve()            → 多通道检索+MCP
  │           │     ├─ MultiChannelRetrievalEngine
  │           │     │     ├─ VectorGlobalSearchChannel  (全局向量搜索)
  │           │     │     ├─ IntentDirectedSearchChannel (意图定向搜索)
  │           │     │     ├─ DeduplicationPostProcessor  (去重)
  │           │     │     └─ RerankPostProcessor         (重排序)
  │           │     └─ MCP工具调用（并行）
  │           │
  │           └─ [7] streamRagResponse()   → LLM流式生成
  │                 ├─ RAGPromptService.buildStructuredMessages()
  │                 │     ├─ 选择模板 (KB/MCP/Mixed)
  │                 │     ├─ system prompt
  │                 │     ├─ history messages
  │                 │     └─ user content (检索证据 + 问题)
  │                 │
  │                 └─ LLMService.streamChat()
  │                       └─ StreamChatEventHandler.onContent()
  │                             └─ SSE push → 客户端
```

## 附录C：关键配置速查

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `rag.vector.type` | `pg` | 向量库类型：pg 或 milvus |
| `rag.query-rewrite.enabled` | `true` | 是否启用 Query 改写 |
| `rag.memory.history-keep-turns` | `4` | 保留最近几轮对话 |
| `rag.memory.summary-start-turns` | `5` | 超过几轮触发摘要 |
| `rag.rate-limit.global.max-concurrent` | `1` | 全局最大并发问答数 |
| `rag.search.channels.vector-global.confidence-threshold` | `0.6` | 全局检索置信度阈值 |
| `rag.search.channels.intent-directed.min-intent-score` | `0.4` | 意图定向检索最低分数 |
| `app.medical.intent-tree.auto-init` | `true` | 启动时自动初始化意图树 |

---


# 第二部分：基础设施模块

## 8. framework/ — 基础设施层（40类）

框架层提供与业务无关的通用能力，被所有其他模块依赖。

### 8.1 config/ — 自动配置

| 文件 | 说明 |
|------|------|
| `DataBaseConfiguration.java` | 数据库配置：注册 MyBatis-Plus 分页插件、自动填充处理器 |
| `RocketMQAutoConfiguration.java` | RocketMQ 自动配置：扫描 `@RocketMQMessageListener` 并注册消费者容器 |
| `WebAutoConfiguration.java` | Web 层自动配置：Jackson 序列化配置 |

### 8.2 context/ — 上下文传递

基于 Alibaba TTL 的上下文传递，解决线程池场景下父子线程数据传递。

| 文件 | 说明 |
|------|------|
| `UserContext.java` | **用户上下文容器**：基于 TTL 存储当前请求的用户信息。`getUserId()`/`getUsername()`/`getRole()` |
| `LoginUser.java` | 登录用户模型：`userId` + `username` + `role` + `avatar` |
| `ApplicationContextHolder.java` | Spring ApplicationContext 持有者：在非 Spring 管理的类中获取 Bean |

### 8.3 convention/ — 通用模型

| 文件 | 说明 |
|------|------|
| `Result.java` | **统一返回体**：`{code, message, data, success}`，所有 API 返回值都包装为 Result |
| `ChatMessage.java` | 对话消息模型：`role`(SYSTEM/USER/ASSISTANT) + `content` + `thinkingContent` |
| `ChatRequest.java` | LLM 请求模型：`messages` + `temperature` + `topP` + `thinking` + `maxTokens` |
| `RetrievedChunk.java` | 检索结果 Chunk：`chunkId` + `content` + `score` + `metadata` + `sourceDocName` |

### 8.4 web/ — Web 层工具

| 文件 | 说明 |
|------|------|
| `Results.java` | 响应构建工具：`Results.success(data)` 快捷创建成功响应 |
| `SseEmitterSender.java` | **SSE 发送器**：封装 Spring SseEmitter，线程安全。`sendEvent(name, data)`/`complete()`/`fail(t)` |
| `GlobalExceptionHandler.java` | **全局异常处理器**：`@RestControllerAdvice`，统一处理异常返回标准 Result |

### 8.5 exception/ — 异常体系

| 文件 | 说明 |
|------|------|
| `AbstractException.java` | 抽象基类：含 `errorCode`、`errorMessage` |
| `ClientException.java` | **A类-用户端错误**（参数校验、权限等） |
| `ServiceException.java` | **B类-系统执行错误**（内部逻辑、数据库操作等） |
| `RemoteException.java` | **C类-第三方服务错误**（AI服务、MCP工具等远程调用失败） |
| `kb/VectorCollectionAlreadyExistsException.java` | 向量集合已存在异常 |

### 8.6 errorcode/ — 错误码

| 文件 | 说明 |
|------|------|
| `IErrorCode.java` | **错误码接口**：`code()` + `message()` |
| `BaseErrorCode.java` | **标准错误码枚举**：A000001(客户端)/B000001(系统)/C000001(第三方)及注册、密码、幂等等细分码 |

### 8.7 idempotent/ — 幂等性

| 文件 | 说明 |
|------|------|
| `IdempotentSubmit.java` | **注解**：标记 Controller 方法，通过 Redis Token 防重复提交 |
| `IdempotentSubmitAspect.java` | **切面**：拦截 `@IdempotentSubmit`，校验并删除 Token |
| `IdempotentConsume.java` | **注解**：标记 MQ 消费方法，防重复消费 |
| `IdempotentConsumeAspect.java` | **切面**：基于 Redis + DB 状态表双重防重 |
| `IdempotentConsumeStatusEnum.java` | 状态枚举：PENDING/CONSUMED |
| `SpELUtil.java` | SpEL 表达式解析工具 |

### 8.8 mq/ — 消息队列

| 文件 | 说明 |
|------|------|
| `MessageWrapper.java` | 消息包装器：统一格式，含 `messageId`/`eventType`/`payload` |
| `producer/MessageQueueProducer.java` | **生产者接口**：`send()`/`sendInTransaction()` |
| `producer/RocketMQProducerAdapter.java` | RocketMQ 生产者适配实现 |
| `producer/DelegatingTransactionListener.java` | 事务消息监听器委托 |
| `producer/TransactionChecker.java` | 事务回查器接口 |

### 8.9 trace/ — 链路追踪

| 文件 | 说明 |
|------|------|
| `RagTraceRoot.java` | **注解**：标记问答顶层方法，创建 Trace 根节点 |
| `RagTraceNode.java` | **注解**：标记子方法，AOP 记录耗时和异常 |
| `RagTraceContext.java` | **Trace 上下文**：基于 TTL，维护 traceId/taskId/nodeStack（用于父子节点关联） |
| `RagStreamTraceSupport.java` | **接口**：跨线程 Stream 节点 Trace 支持 |
| `NoopRagStreamTraceSupport.java` | 空操作实现：默认 Bean |

### 8.10 distributedid/ — 分布式ID

| 文件 | 说明 |
|------|------|
| `SnowflakeIdInitializer.java` | **雪花ID初始化器**：启动时计算 workerId/datacenterId，注入 MyBatis-Plus |
| `CustomIdentifierGenerator.java` | 自定义 ID 生成器：基于雪花算法生成全局唯一 Long 型 ID |

### 8.11 cache/ + database/ — 缓存与数据库

| 文件 | 说明 |
|------|------|
| `cache/RedisKeySerializer.java` | Redis Key 序列化器 |
| `database/MyMetaObjectHandler.java` | MyBatis-Plus 自动填充处理器（createTime/updateTime/deleted） |

## 9. infra-ai/ — AI 基础设施层（45类）★★★

封装所有 AI 服务调用。模板方法模式，多提供商支持+断路器+首包探测。

### 9.1 chat/ — 对话客户端

#### 接口层

| 文件 | 说明 |
|------|------|
| `ChatClient.java` | **接口**：`chat(request, target) → String`、`streamChat(request, callback, target) → Handle` |
| `LLMService.java` | **服务接口**：`chat(request)`、`streamChat(request, callback)` |
| `RoutingLLMService.java` | **★★★ 路由 LLM 服务**：首包探测(60秒超时)+模型故障自动切换。`resolveTarget()`→`executeWithFallback()`→流式调用 |
| `StreamCallback.java` | **流式回调接口**：`onContent(chunk)`/`onThinking(chunk)`/`onComplete()`/`onError(t)` |
| `StreamCancellationHandle.java` | 取消句柄接口：`cancel()` 中断流式响应 |
| `StreamCancellationHandles.java` | 组合取消句柄 |

#### 模板方法基类

| 文件 | 说明 |
|------|------|
| `AbstractOpenAIStyleChatClient.java` | **★★★ 核心基类(290行)**：实现 OpenAI 兼容协议。`doChat()`（同步）和 `doStreamChat()`（流式）。子类只需覆写 `provider()` |
| `OpenAIStyleSseParser.java` | **SSE 解析器**：解析 `data: {...}` 流，提取 `delta.content` 和 `reasoning_content` |
| `StreamAsyncExecutor.java` | 流式异步执行器：在线程池中执行 SSE 读取循环 |
| `StreamSpanCallback.java` | Stream Span 回调包装 |

#### 具体提供商实现

| 文件 | 说明 |
|------|------|
| `SiliconFlowChatClient.java` | 硅基流动（当前使用） |
| `BaiLianChatClient.java` | 阿里云百炼 |
| `OllamaChatClient.java` | 本地 Ollama |
| `AIHubMixChatClient.java` | AIHubMix 聚合平台 |

#### 探测与桥接

| 文件 | 说明 |
|------|------|
| `ProbeStreamBridge.java` | **首包探测桥接器**：首包到达前缓冲数据，确认后释放。用于验证模型是否存活 |
| `LlmFirstPacketProbe.java` | 首包探测节点：AOP 记录 TTFT（Time To First Token） |
| `ForwardingStreamCallback.java` | 转发回调 |

### 9.2 embedding/ — 向量化服务

| 文件 | 说明 |
|------|------|
| `EmbeddingClient.java` | **客户端接口**：`embed(text, target) → List<Float>`、`embedBatch(texts, target)` |
| `EmbeddingService.java` | **服务接口**：`embed(text)`、`embed(text, modelId)` |
| `RoutingEmbeddingService.java` | **★★ 路由服务**：按优先级选择候选，支持故障切换 |
| `AbstractOpenAIStyleEmbeddingClient.java` | **抽象基类**：实现 `/v1/embeddings` 协议。维度参数可配置 |
| `SiliconFlowEmbeddingClient.java` | 硅基流动（当前使用 BAAI/bge-large-zh-v1.5） |
| `AIHubMixEmbeddingClient.java` | AIHubMix |
| `OllamaEmbeddingClient.java` | Ollama 本地 |

### 9.3 rerank/ — 重排序服务

| 文件 | 说明 |
|------|------|
| `RerankClient.java` | 客户端接口 |
| `RerankService.java` | 服务接口 |
| `RoutingRerankService.java` | **路由服务**：按优先级选择候选 |
| `BaiLianRerankClient.java` | 百炼 Rerank 客户端 |
| `NoopRerankClient.java` | 空操作 fallback（priority=100） |

### 9.4 model/ — 模型路由与健康检查 ★★

| 文件 | 说明 |
|------|------|
| `ModelHealthStore.java` | **★★ 断路器**：三状态(CLOSED→OPEN→HALF_OPEN)，CAS 原子状态转换+半开探针信号量 |
| `ModelRoutingExecutor.java` | **★★ 路由执行器**：按 priority 排序→健康检查→执行→成功提交/失败切换 |
| `ModelSelector.java` | 模型选择器：根据 modelId 找到候选 |
| `ModelTarget.java` | 模型目标：包装候选+配置参数 |
| `ModelCaller.java` | 函数式接口：`call(client, target) → T` |

**三状态断路器：**
```
CLOSED(正常) → 连续失败2次 → OPEN(熔断30秒) → HALF_OPEN(探测) → 成功→CLOSED | 失败→OPEN
```

### 9.5 token/ — Token 计数

| 文件 | 说明 |
|------|------|
| `TokenCounterService.java` | 接口 |
| `HeuristicTokenCounterService.java` | 启发式实现：中文~1.5字/token，英文~4字/token |

### 9.6 http/ — HTTP 工具

| 文件 | 说明 |
|------|------|
| `ModelUrlResolver.java` | **URL 解析器**：拼装完整 API URL |
| `HttpResponseHelper.java` | 响应工具：`parseJson()`/`readBody()`/`requireProvider()`/`requireApiKey()` |
| `ModelClientException.java` | 模型客户端异常：含 `errorType` |
| `ModelClientErrorType.java` | 错误类型枚举：NETWORK_ERROR/PROVIDER_ERROR/INVALID_RESPONSE 等 |
| `HttpMediaTypes.java` | 媒体类型常量 |

### 9.7 config/ + enums/ + util/ — 配置与工具

| 文件 | 说明 |
|------|------|
| `config/AIModelProperties.java` | **★★ AI 配置属性类**：映射 `ai.*` 全部配置——providers、chat/embedding/rerank候选、断路器参数 |
| `enums/ModelProvider.java` | 提供商枚举：OLLAMA/BAILIAN/AIHUBMIX/SILICON_FLOW/NOOP |
| `enums/ModelCapability.java` | 能力枚举：CHAT/EMBEDDING/RERANK |
| `util/LLMResponseCleaner.java` | LLM 响应清洗：去除 Markdown 代码块标记，提取纯 JSON |

## 10. mcp-server/ — MCP工具服务器（3类）

独立的 Spring Boot 进程，提供 MCP 工具服务。Bootstrap 通过 HTTP 调用。

| 文件 | 说明 |
|------|------|
| `McpServerApplication.java` | 启动类 |
| `config/McpServerConfig.java` | MCP Server 配置：注册所有 `SyncToolSpecification` Bean |
| `executor/ExampleMcpExecutor.java` | **MCP 工具示例**：演示注册模式——通过 `@Bean` 返回 `SyncToolSpecification`（工具 JSON Schema + 执行回调），项目可参考添加自定义 MCP 工具 |

**MCP 工具注册模式：**
```java
@Bean
public SyncToolSpecification myTool() {
    Tool tool = Tool.builder().name("tool_id").description("...").inputSchema(schema).build();
    return new SyncToolSpecification(tool, (exchange, request) -> {
        Map<String, Object> args = request.arguments();
        String result = doBusinessLogic(args);
        return CallToolResult.builder().content(List.of(new TextContent(result))).build();
    });
}
```

---

## 附录D：全部模块依赖关系

```
mcp-server ─────────────────────┐
                                │ (HTTP/MCP协议)
framework ◄── infra-ai ◄── bootstrap ── mcp-server
   │             │            │
   └── 基础层 ────┘            └── 应用层
```

| 模块 | 文件数 | 层级 | 职责 |
|------|--------|------|------|
| framework | 40 | 基础层 | 上下文、异常、幂等、MQ、Trace、分布式ID |
| infra-ai | 45 | AI层 | Chat/Embedding/Rerank 客户端、模型路由、断路器 |
| mcp-server | 3 | 工具层 | MCP 工具服务器，独立进程 |
| bootstrap | 359 | 应用层 | RAG 管线、知识库管理、文档摄取、意图分类 |

## 附录E：infra-ai 完整调用链

```
bootstrap.StreamChatPipeline.streamLLMResponse()
  │
  └─ LLMService.streamChat(request, callback)
      └─ RoutingLLMService.streamChat()
          ├─ ModelSelector.resolve(modelId)        → 候选列表
          ├─ ModelRoutingExecutor.executeWithFallback()
          │     ├─ ModelHealthStore.preCheck()      → 断路器检查
          │     ├─ AbstractOpenAIStyleChatClient.doStreamChat()
          │     │     ├─ 构建 OpenAI 请求体
          │     │     ├─ OkHttp 发起 SSE 请求
          │     │     └─ OpenAIStyleSseParser 解析 SSE
          │     ├─ ProbeStreamBridge 首包探测 (60s超时)
          │     ├─ [成功] → StreamCallback.onContent(chunk)
          │     └─ [失败] → ModelHealthStore.recordFailure() → 切换下个候选
          └─ StreamCallback.onComplete()
```

## 附录F：全项目配置速查

| 配置项 | 默认值 | 模块 | 说明 |
|--------|--------|------|------|
| `rag.vector.type` | `pg` | bootstrap | 向量库类型 |
| `rag.memory.history-keep-turns` | `4` | bootstrap | 保留对话轮数 |
| `rag.rate-limit.global.max-concurrent` | `1` | bootstrap | 最大并发问答 |
| `ai.selection.failure-threshold` | `2` | infra-ai | 断路器熔断阈值 |
| `ai.selection.open-duration-ms` | `30000` | infra-ai | 熔断恢复时间 |
| `ai.stream.message-chunk-size` | `1` | infra-ai | SSE 块大小 |
| 首包超时 | `60s` | infra-ai | 硬编码在 RoutingLLMService |
| `app.medical.intent-tree.auto-init` | `true` | bootstrap | 启动时初始化意图树 |
