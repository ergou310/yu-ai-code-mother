# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI code generation platform (鱼皮AI代码生成器). Users describe what they want via prompts, and the system generates runnable web code (single HTML or multi-file HTML/CSS/JS) using LLM, then saves the output to disk.

## Tech Stack

**Backend:** Spring Boot 3.5, Java 21, MyBatis-Flex, LangChain4j, MySQL, Hutool, Knife4j (OpenAPI docs)
**Frontend:** Vue 3, TypeScript, Vite 8, Ant Design Vue, Pinia, Vue Router

## Build & Run Commands

### Backend (Maven)
```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run
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
npm run dev        # dev server
npm run build      # type-check + production build
npm run lint       # oxlint + eslint
npm run format     # prettier
```

## Architecture

### AI Code Generation Pipeline

The core flow lives in `core/` and uses Strategy + Template Method + Executor patterns:

1. **`AiCodeGeneratorService`** (interface, `ai/`) — LangChain4j `@AiServices` interface. System prompts are in `src/main/resources/prompt/`. Two modes: `generateHtmlCode` (returns `HtmlCodeResult`) and `generateMultiFileCode` (returns `MultiFileCodeResult`). Each has a streaming variant returning `Flux<String>`.

2. **`AiCodeGeneratorServiceFactory`** — Spring `@Configuration` that builds the LangChain4j proxy with both sync `ChatModel` and async `StreamingChatModel`.

3. **`AiCodeGeneratorFacade`** (`core/`) — Unified entry point. For sync: calls service → parses → saves. For streaming: collects chunks via `Flux`, then parses and saves in `doOnComplete`.

4. **Parsers** (`core/parser/`) — `CodeParser<T>` strategy interface. `HtmlCodeParser` and `MultiFileCodeParser` parse raw AI output into typed result objects. `CodeParserExecutor` dispatches by `CodeGenTypeEnum`.

5. **Savers** (`core/saver/`) — `CodeFileSaverTemplate<T>` template method (validate → build unique dir → save files). `HtmlCodeFileSaverTemplate` and `MultiFileCodeFileSaverTemplate` implement `saveFiles()`. Files go to `{project}/tmp/code_output/{type}_{snowflakeId}/`. `CodeFileSaverExecutor` dispatches by type.

### Code Gen Types

`CodeGenTypeEnum` defines: `HTML` (single self-contained HTML file) and `MULTI_FILE` (separate HTML/CSS/JS files). The AI result models (`ai/model/`) use LangChain4j `@Description` for structured output.

### User Module

Standard CRUD with session-based auth. `@AuthCheck` annotation + `AuthInterceptor` AOP for role-based access control (`user`/`admin`). Sessions stored in `HttpServletRequest`.

### Package Layout

```
com.yupi.yuaicodemother
├── ai/              # LangChain4j service interface, factory, result models
├── annotation/      # Custom annotations (@AuthCheck)
├── aop/             # AOP interceptors (AuthInterceptor)
├── commen/          # BaseResponse, ResultUtils, request DTOs
├── config/          # CORS, JSON config
├── constant/        # Constants (UserConstant)
├── controller/      # REST controllers
├── core/            # Code generation facade, parsers, savers
├── exception/       # ErrorCode enum, BusinessException, ThrowUtils
├── generator/       # MyBatis-Flex code generator
├── mapper/          # MyBatis-Flex mappers
├── model/           # entity/, dto/, vo/, enums/
└── service/         # Service interfaces + impl/
```

## Key Configuration

- Server runs on port `8123` with context path `/api`
- AI model: DeepSeek v4 Pro via OpenAI-compatible API (configured in `application-local.yml`)
- DB: MySQL `yu_ai_code_mother` on localhost:3306
- API docs: Knife4j at `/api/doc.html`
