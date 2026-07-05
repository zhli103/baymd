# BayMD — AI 私人医生

> 基于 ReAct Agent 驱动的医学健康智能问答系统

BayMD 是一个面向 C 端用户的 **AI 私人医生**，覆盖 7 大医学场景，支持流式对话、自主推理与长期记忆进化。

## 核心能力

### 🤖 ReAct Agent 智能体
LLM 自主决策——思考-行动-观察循环，动态编排知识库检索与 MCP 工具调用，最大 10 轮迭代，全时钟超时保护 + 强制收敛。

### 🧠 渐进式记忆系统
三级记忆架构，越用越聪明：
- **短时记忆** — 滑动窗口保留最近 N 轮对话
- **摘要压缩** — 超阈值自动触发 LLM 摘要，压缩早期对话为结构化摘要
- **语义记忆** — 异步抽取 Fact（健康事实）与 Episode（对话片段），向量化存入 HNSW 索引，对话前检索相关记忆注入 Prompt
- **记忆进化** — Fact 自动合并去重消歧，置信度随反馈动态演化，累积超 30 条自动生成用户健康画像

### 🎯 7 大医学场景
| 场景 | 说明 |
|------|------|
| 科室推荐 | 根据症状推荐就诊科室（内科/外科/皮肤/妇产儿） |
| 症状自查 | 症状病因分析、严重程度评估与就医建议 |
| 药物查询 | 西药与中成药的功效、用法、副作用、禁忌 |
| 饮食建议 | 慢病饮食、消化调理、日常营养指导 |
| 中医辨证 | 体质辨识、常见症状辨证、中医食疗 |
| 报告解读 | 血常规、生化指标、尿常规等体检报告分析 |
| 医院推荐 | 医院等级选择、专科推荐、就诊指南 |

### 🔍 多通道混合检索
全局向量检索 + 意图定向检索并行执行，RRF 倒序排名融合去重后返回最优结果。

### 🛡️ Agent 安全护栏
工具调用三重保障（重试 / 超时 / 降级）+ Checkpoint 检查点 + 证据预算低置信度兜底。

### 📄 文档摄取 DAG
6 节点可编排流水线：Fetcher → Parser → Enhancer → Chunker → Enricher → Indexer，支持 PDF/DOCX/HTML/Markdown，定时刷新与变更检测。

### 🏥 模型健康路由
三状态断路器（CLOSED → OPEN → HALF_OPEN），首包探活，故障自动切换。

## 7 阶段流式对话管线

```
Memory Load → Query Rewrite → Intent Classification → Ambiguity Guidance
→ System-Only Check → Retrieval → Stream Response (SSE)
```

通过执行器注册表按优先级分发：歧义引导 → 系统直答 → Agent → RAG。

## 快速开始

```bash
# 1. 启动依赖服务
docker compose -f resources/docker/lightweight/milvus-stack-2.6.6.compose.yaml up -d

# 2. 初始化数据库（需要 pgvector 扩展）
psql -h localhost -U postgres -d baymd -f resources/database/schema_pg.sql

# 3. 配置 API Key
export BAILIAN_API_KEY=your_key_here

# 4. 启动服务
./mvnw spring-boot:run -pl bootstrap

# 5. 发起问答（SSE 流式）
curl "http://localhost:9090/api/baymd/rag/v3/chat?question=头疼应该挂什么科"
```

## 项目结构

| 模块 | 说明 |
|------|------|
| `framework` | 基础设施层：分布式限流、AOP 切面、上下文传递、通用工具 |
| `infra-ai` | AI 基础设施：Chat Client 抽象、模型路由、健康检查、流式响应 |
| `mcp-server` | MCP 工具服务：独立 MCP Server 进程，可注册任意工具 |
| `bootstrap` | 应用启动层：RAG 核心逻辑、知识库管理、文档摄取、API 接口 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 & 框架 | JDK 17, Spring Boot 3.5.7 |
| ORM & 数据库 | MyBatis-Plus 3.5.14, PostgreSQL + pgvector |
| 向量数据库 | Milvus 2.6.6 |
| 缓存 & 限流 | Redisson 4.0（分布式公平队列限流） |
| 消息队列 | RocketMQ |
| 文档解析 | Apache Tika 3.2.3 |
| 认证鉴权 | Sa-Token 1.43.0 |
| MCP 协议 | MCP SDK 1.1.2 |
| 对象存储 | AWS SDK S3 2.40.2 |
| CI/CD | GitHub Actions |

## License

Apache License 2.0
