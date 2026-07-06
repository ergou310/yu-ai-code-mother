# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI code generation platform (鱼皮AI代码生成器). Users describe what they want via prompts, and the system generates runnable web code using LLM, then saves the output to disk. Supports three generation modes: single HTML, multi-file HTML/CSS/JS, and Vue projects.

## Tech Stack

**Backend:** Spring Boot 3.5.15, Java 21, MyBatis-Flex 1.11, LangChain4j 1.1.0, MySQL, Redis (session + chat memory), Hutool, Knife4j (OpenAPI docs), Lombok
**Frontend:** Vue 3, TypeScript, Vite 7, Ant Design Vue 4, Pinia 3, Vue Router 4, Axios, markdown-it

## Build & Run Commands

### Backend (Maven)
```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run (requires MySQL on localhost:3306 and Redis on localhost:6379)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=YuAiCodeMotherApplicationTests
```

### Frontend
```bash
cd yu-ai-code-mother-frontend
npm install
npm run dev          # dev server (proxies to backend at localhost:8123/api)
npm run build        # type-check + production build
npm run lint         # eslint with auto-fix
npm run format       # prettier
npm run openapi2ts   # regenerate API types from backend OpenAPI spec
```

## Architecture

### AI Code Generation Pipeline

The core flow lives in `core/` and uses Strategy + Template Method + Executor patterns:

1. **`AiCodeGeneratorService`** (`ai/`) — LangChain4j `@AiServices` interface. System prompts are in `src/main/resources/prompt/`. Three modes: `generateHtmlCode`, `generateMultiFileCode`, and Vue project generation. Each has a streaming variant returning `Flux<String>`.

2. **`AiCodeGeneratorServiceFactory`** (`ai/`) — Spring `@Configuration` that builds the LangChain4j proxy with both sync `ChatModel` and async `StreamingChatModel`. Uses DeepSeek v4 Pro via OpenAI-compatible API.

3. **`AiCodeGeneratorFacade`** (`core/`) — Unified entry point. For sync: calls service → parses → saves. For streaming: dispatches to stream handlers, then parses and saves on completion.

4. **Stream Handlers** (`core/handler/`) — `StreamHandlerExecutor` dispatches between `SimpleTextStreamHandler` (plain text responses) and `JsonMessageStreamHandler` (structured JSON with tool call support). The JSON handler emits `StreamMessage` events (`AiResponseMessage`, `ToolRequestMessage`, `ToolExecutedMessage`) via SSE.

5. **AI Tools** (`ai/tools/`) — LangChain4j tool implementations. `FileWriteTool` enables the AI to write files during generation (tool calling pattern).

6. **Parsers** (`core/parser/`) — `CodeParser<T>` strategy interface. `HtmlCodeParser` and `MultiFileCodeParser` parse raw AI output into typed result objects. `CodeParserExecutor` dispatches by `CodeGenTypeEnum`.

7. **Savers** (`core/saver/`) — `CodeFileSaverTemplate<T>` template method (validate → build unique dir → save files). `HtmlCodeFileSaverTemplate` and `MultiFileCodeFileSaverTemplate` implement `saveFiles()`. Files go to `{project}/tmp/code_output/{type}_{appId}/`. `CodeFileSaverExecutor` dispatches by type.

### Code Gen Types

`CodeGenTypeEnum` defines three modes:
- `HTML` — single self-contained HTML file
- `MULTI_FILE` — separate HTML/CSS/JS files
- `VUE_PROJECT` — full Vue project structure with tool calling

The AI result models (`ai/model/`) use LangChain4j `@Description` annotations for structured output.

### Chat History Module

`ChatHistory` entity stores conversation messages per app. Uses cursor-based pagination (`appId + createTime` composite index). `ChatHistoryService` handles both user and AI messages. LangChain4j chat memory is backed by Redis (`RedisChatMemoryStoreConfig`).

### User & App Modules

Standard CRUD with session-based auth stored in Redis (30-day TTL). `@AuthCheck` annotation + `AuthInterceptor` AOP for role-based access control (`user`/`admin`). The `App` entity tracks `codeGenType`, `deployKey`, and `priority` (for featured apps).

### Deployment

Generated code is saved to `tmp/code_output/`. The `code_deploy/` directory holds deployed app artifacts keyed by `deployKey`. `StaticResourceController` serves generated code for preview.

### Database Schema

SQL schema is in `sql/create_table.sql`. Three tables: `user`, `app`, `chat_history`. Run this script to initialize the `yu_ai_code_mother` database.

### Package Layout

```
com.yupi.yuaicodemother
├── ai/              # LangChain4j service interface, factory, tools, result models
│   ├── model/       # HtmlCodeResult, MultiFileCodeResult, message/ (SSE events)
│   └── tools/       # FileWriteTool (AI tool calling)
├── annotation/      # @AuthCheck
├── aop/             # AuthInterceptor
├── commen/          # BaseResponse, ResultUtils, request DTOs (note: typo in package name is intentional)
├── config/          # CORS, JSON, Redis chat memory, streaming model config
├── constant/        # UserConstant, AppConstant
├── controller/      # AppController, UserController, ChatHistoryController, StaticResourceController
├── core/            # Code generation facade, parsers, savers, stream handlers
├── exception/       # ErrorCode enum, BusinessException, ThrowUtils
├── generator/       # MyBatis-Flex code generator
├── mapper/          # MyBatis-Flex mappers
├── model/           # entity/, dto/, vo/, enums/
└── service/         # Service interfaces + impl/
```

## Frontend Architecture

### API Layer (`src/api/`)
Auto-generated from backend OpenAPI spec via `npm run openapi2ts`. Each backend controller maps to a TypeScript file (e.g., `appController.ts`, `userController.ts`). Type definitions live in `typings.d.ts`. **Do not manually edit files in `src/api/`** — regenerate them instead.

### Streaming (SSE)
`AppChatPage.vue` uses `EventSource` to consume the `/app/chat/gen/code` endpoint. Messages arrive as `{"d": "..."}` JSON chunks. The page accumulates content and renders it via `MarkdownRenderer`. A `"done"` event signals completion and triggers preview refresh.

### Environment Config (`src/config/env.ts`)
Defines `API_BASE_URL` and `DEPLOY_DOMAIN`. Override via `VITE_API_BASE_URL` env var. `getStaticPreviewUrl()` builds the iframe preview URL from these.

### Access Control (`src/access.ts`)
Global `router.beforeEach` guard fetches the logged-in user on first load and blocks `/admin/*` routes for non-admin users.

## Development Notes

### Custom LangChain4j Classes
The project includes patched classes under `src/main/java/dev/langchain4j/` (in `model/chat/response/`, `model/openai/`, `internal/`, `service/` packages). These are compatibility fixes for the streaming model — avoid modifying unless you understand the upstream LangChain4j internals.

### Vue Project Generation Constraints
The system prompt for Vue project generation enforces strict limits: `<20000 tokens`, `<30 files`, no state management libraries, hash-mode routing (`#/`), and `base: './'` in `vite.config.js`. These constraints exist because the AI generates the entire project in one pass via tool calling.

### Virtual Threads
`VueProjectBuilder` uses Java 21 virtual threads (`Thread.ofVirtual()`) for async project builds. This requires `--enable-preview` (already configured in `pom.xml`).

### Deployment Artifacts
- Generated code: `tmp/code_output/{codeGenType}_{appId}/`
- Deployed apps: `tmp/code_deploy/{deployKey}/` (copied on deploy, Vue projects include built `dist/`)
- `StaticResourceController` serves both for preview

## Key Configuration

- Server: port `8123`, context path `/api`
- AI: DeepSeek v4 Pro via OpenAI-compatible API (`application-local.yml`)
- DB: MySQL `yu_ai_code_mother` on localhost:3306 (root/123456)
- Redis: localhost:6379 (session store + LangChain4j chat memory)
- API docs: Knife4j at `/api/doc.html`
- Frontend API base: `http://localhost:8123/api` (configurable via `VITE_API_BASE_URL` env var)
- Static preview: `{API_BASE_URL}/static/{codeGenType}_{appId}/`
- `--enable-preview` JVM flag required (virtual threads)
