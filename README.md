# FlowWork — AI 驱动团队项目协作平台

> 基于 Spring AI Alibaba 构建的 AI Agent + 业务系统深度融合的全栈项目

## 项目简介

FlowWork 是一款面向中小团队的 AI 驱动项目协作平台，核心特性：
- 🤖 **AI 自动任务拆解**：一句话生成项目结构、阶段、依赖关系
- 🛠️ **ToolCalling 业务执行**：AI 调用后端 Java 方法直接操作数据库
- 📝 **AI 会议 → 任务**：会议内容自动提取 Action Items 并入库
- 📚 **项目知识库 RAG**：文档向量化 + 自然语言检索
- 🔄 **WebSocket 实时协作**：看板拖拽、状态变更全员实时同步
- 🔐 **RBAC 权限体系**：完整三级权限模型 + 前后端权限控制

## 技术栈

```
后端: Spring Boot 3.4.x + Spring AI Alibaba 1.1.2.2 + MyBatis-Plus + Spring Security 6
数据: MySQL 8.0.x + Redis 7.x
前端: Vue 3.4+ + TypeScript 5.x + Vite 5.x + Pinia 2.x + Element Plus 2.x
实时: WebSocket + STOMP + Redis Pub/Sub
AI:  ReactAgent + Graph 工作流 + VectorStore RAG
```

## 目录结构

```
item2/
├── README.md                          ← 本文件
├── project.json                       ← 项目元数据
├── 第二个项目 FlowWork 最终总结（技术栈、亮点、区别、新增技术）.md
│
├── specs/                             ← V3.2 Spec 规范目录
│   ├── overview/                      ← 第一阶段：总体设计（必须先通过）
│   │   ├── requirements.md            ← EARS 需求文档
│   │   ├── design.md                  ← 技术架构与选型
│   │   ├── tasks.md                   ← 开发排期与优先级
│   │   └── status.yaml                ← 状态元数据
│   │
│   ├── auth/                          ← 模块：认证授权（RBAC）
│   ├── project/                       ← 模块：项目 + 任务看板
│   ├── ai/                            ← 模块：AI Agent 核心能力
│   ├── knowledge/                     ← 模块：项目知识库 RAG
│   ├── realtime/                      ← 模块：WebSocket 实时协作
│   └── index.md                       ← 全项目模块进度导航
│
├── backend/                           ← 第二阶段：后端代码
└── frontend/                          ← 第二阶段：前端代码
```

## 开发流程（V3.2 Spec 驱动）

```
Step 1: 生成 overview 总体设计 ──→ 用户确认 approved
Step 2: 逐模块生成 Spec ──→ 每模块用户确认 approved
Step 3: 按 tasks.md 顺序开发代码
Step 4: 每完成一个任务更新状态
```

## 快速开始

（待补充 — overview approved 后在此处填写启动命令）

---

**项目一 + 项目二 组合**：AI 心理助手（后端 AI 深度） + FlowWork（全栈工程广度），简历互补拉满。