# BayMD — AI 私人医生

> 基于 ReAct Agent + 记忆系统的医学健康智能问答系统，**越用越聪明**

[![CI](https://github.com/zhli103/baymd/actions/workflows/ci.yml/badge.svg)](https://github.com/zhli103/baymd/actions/workflows/ci.yml)

## 核心亮点

### 🤖 ReAct Agent 自主推理
LLM 自主决策：思考 → 工具调用 → 观察结果 → 循环，最大 10 轮迭代。知识库检索和 MCP 工具平等对待，Agent 自主选择调用时机。

```
用户: 头疼和发烧分别怎么办
Agent: 思考 → search_knowledge_base("头疼") → 观察结果
     → search_knowledge_base("发烧") → 观察结果
     → 综合分析 → 最终回答
```

### 🧠 渐进式记忆系统（借鉴 EverOS）
三级记忆架构，越用越了解用户：

| 层级 | 策略 | 说明 |
|------|------|------|
| 短时 | sliding_window | 滑动窗口保留最近 N 轮原文 |
| 长时 | summary_compression | 超阈值自动 LLM 摘要压缩 |
| 语义 | semantic | 异步抽取 Fact + Episode → 向量化 → HNSW 检索 → 注入 Prompt |

**记忆生命周期**：
```
对话结束 → LLM 提取 AtomicFact（原子事实）+ Episode（情节摘要）
→ SHA-256 去重 → pgvector 向量化入库
→ 下次对话 → 语义检索相关记忆 → 注入 Prompt

累积 10+ 同类型 Fact → LLM 自动合并去重
累积 30+ Fact → 自动生成用户健康画像
```

配置切换：`rag.memory.strategy: semantic`

### 🎯 7 大医学场景

| 场景 | 说明 |
|------|------|
| 科室推荐 | 根据症状推荐就诊科室 |
| 症状自查 | 症状病因分析与就医建议 |
| 药物查询 | 功效、用法、副作用、禁忌 |
| 饮食建议 | 慢病饮食、营养指导 |
| 中医辨证 | 体质辨识、中医食疗 |
| 报告解读 | 血常规、生化指标分析 |
| 医院推荐 | 医院等级与专科推荐 |

### 🔍 多通道混合检索

向量全局检索 + 意图定向检索并行 → **RRF 倒序排名融合**（k=60）→ Rerank 精排 → 证据预算截断

### 🛡️ 生产级护栏

- **工具调用**：指数退避重试 + 超时 + 降级（RetrievalEngine/MCP 全覆盖）
- **证据预算**：Token 预算控制 + 低置信度短路
- **Checkpoint**：Redis 持久化，支持中断恢复
- **模型路由**：三状态断路器（CLOSED → OPEN → HALF_OPEN），故障自动切换

### 🎨 React 前端

独立 React + Vite + TypeScript + Tailwind CSS 前端，支持：

- SSE 流式对话 + 思考过程展示
- 会话列表管理（新建/切换/删除/导出）
- 知识库管理
- 系统设置（含清空记忆、链路追踪）
- 点赞/踩反馈联动

### 📄 其他

- **文档摄取 DAG**：6 节点可编排流水线，支持 PDF/DOCX/HTML/MD
- **查询改写 + 子问题拆分**：LLM 改写 + Jaccard 去重 + 自包含增强 + 质量评分
- **推荐追问 + 引用来源**：回答后自动生成追问，SSE 完成帧附带引用
- **LLM-as-Judge**：异步四维质量评分（准确度/完整度/忠实度/简洁度）
- **反馈联动**：点踩自动降级关联 Fact，持续踩自动清理

## 对话管线

```
Memory Load → Query Rewrite → Intent Classification
→ ExecutorRegistry 分发:
    ├── ClarificationExecutor   (歧义引导)
    ├── SystemOnlyExecutor      (闲聊/问候，不走检索)
    ├── AgentExecutor           (ReAct 循环，rag.react.enabled=true)
    └── RagExecutor             (经典 RAG，默认)
```

## 快速开始

```bash
# 1. 启动依赖服务（PostgreSQL + Redis + RocketMQ + Milvus）
docker compose -f resources/docker/lightweight/milvus-stack-2.6.6.compose.yaml up -d

# 2. 初始化数据库
psql -h localhost -U postgres -d baymd -f resources/database/schema_pg.sql

# 3. 配置 LLM API Key
export BAILIAN_API_KEY=your_key_here

# 4. 启动后端（端口 9090）
./mvnw spring-boot:run -pl bootstrap

# 5. 启动前端（端口 5173，代理到 9090）
cd frontend && npm install && npm run dev

# 6. 发起问答
curl --get "http://localhost:9090/api/baymd/rag/v3/chat" \
  --data-urlencode "question=头疼挂什么科"
```

## 项目结构

| 模块 | 说明 |
|------|------|
| `framework` | 基础设施：分布式限流、AOP、上下文传递 |
| `infra-ai` | AI 基础设施：Chat/Embedding 客户端、模型路由、断路器 |
| `mcp-server` | MCP 工具服务 |
| `bootstrap` | 应用核心：RAG 管线、Agent、记忆系统、API |
| `frontend` | React 前端（Vite + Tailwind CSS） |

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | JDK 17 / TypeScript |
| 框架 | Spring Boot 3.5.7 / React + Vite |
| 数据库 | PostgreSQL + pgvector (HNSW) |
| 向量库 | Milvus 2.6.6（可选） |
| 缓存 | Redisson 4.0 |
| 消息 | RocketMQ |
| 文档 | Apache Tika 3.2.3 |
| 认证 | Sa-Token 1.43.0 |
| MCP | MCP SDK 1.1.2 |
| 存储 | AWS S3 / MinIO |
| CI | GitHub Actions |

## API 一览

```
# 对话
GET  /rag/v3/chat              SSE 流式问答
POST /rag/v3/stop              停止任务

# 会话
GET    /conversations            列表
PUT    /conversations/{id}       重命名
DELETE /conversations/{id}       删除
GET    /conversations/{id}/messages  消息列表
GET    /conversations/{id}/export    导出 JSON
DELETE /memory                   清空记忆

# 反馈
POST /conversations/messages/{id}/feedback  点赞/踩 → 联动 Fact

# 知识库
POST   /knowledge-base               创建
GET    /knowledge-base               列表
DELETE /knowledge-base/{id}          删除
POST   /knowledge-base/{id}/docs/upload  上传文档
GET    /knowledge-base/{id}/docs         文档列表

# 系统
GET /rag/settings         配置
GET /rag/traces/runs      Trace 链路
GET /admin/dashboard      仪表盘
```

## License

Apache License 2.0
