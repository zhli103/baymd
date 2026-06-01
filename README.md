# BayMD

> 基于通用 RAG 框架构建的智能问答助手

BayMD 是一个功能完整的 **RAG（Retrieval-Augmented Generation）** 问答助手，建立在可复用的基础设施之上，支持：

- 🔍 **多通道混合检索** — 并行向量检索 + 意图定向检索，结果去重重排序
- 🧠 **7 阶段流式对话管线** — 记忆加载 → Query 改写 → 意图分类 → 歧义引导 → 系统响应 → 检索 → 流式输出
- 🎯 **树形意图分类** — DOMAIN → CATEGORY → TOPIC 三级意图树，支持 KB / SYSTEM / MCP 三种节点
- 📄 **文档摄取 DAG** — Fetcher → Parser → Enhancer → Chunker → Enricher → Indexer
- 🔧 **MCP 工具集成** — 基于 Model Context Protocol 的实时数据获取
- 🏥 **模型健康路由** — 三状态断路器 + 自动故障切换
- 📝 **多格式文档解析** — PDF, DOC, DOCX, HTML, Markdown
- 🗄️ **双向量存储** — PostgreSQL pgvector + Milvus
- 💬 **对话记忆管理** — 短时记忆 + 长时摘要

## 快速开始

```bash
# 1. 启动依赖服务
docker compose -f resources/docker/lightweight/milvus-stack-2.6.6.compose.yaml up -d

# 2. 执行数据库初始化
psql -h localhost -U postgres -d baymd -f resources/database/schema_pg.sql

# 3. 配置 API Key
export BAILIAN_API_KEY=your_key_here

# 4. 启动服务
./mvnw spring-boot:run -pl bootstrap

# 5. 发起问答
curl "http://localhost:9090/api/baymd/rag/v3/chat?question=你好"
```

## 项目结构

| 模块 | 说明 |
|------|------|
| `framework` | 基础设施层：分布式限流、AOP 切面、上下文传递、通用工具 |
| `infra-ai` | AI 基础设施：Chat Client 抽象、模型路由、健康检查、流式响应 |
| `mcp-server` | MCP 工具服务：独立的 MCP Server 进程，可注册任意工具 |
| `bootstrap` | 应用启动层：RAG 核心逻辑、知识库管理、文档摄取、API 接口 |

## 技术栈

- **JDK 17**, **Spring Boot 3.5.7**
- **MyBatis-Plus** + **PostgreSQL** (pgvector)
- **Milvus** / **Redis** / **RocketMQ**
- **Apache Tika** (文档解析)
- **Sa-Token** (认证鉴权)
- **MCP SDK** (工具集成)
