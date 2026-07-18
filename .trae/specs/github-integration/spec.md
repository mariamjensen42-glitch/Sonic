# GitHub 功能集成 Spec

## Why
项目目前缺少 GitHub 的核心功能配置，无法利用 GitHub 提供的 CI/CD、Issue 管理、PR 流程等能力，影响开发效率和项目质量保障。

## What Changes
- 创建 GitHub Actions CI 工作流（Android 构建、测试、Lint 检查）
- 创建 Issue 模板（Bug 报告、功能请求）
- 创建 Pull Request 模板
- 配置 Dependabot 自动依赖更新
- 创建贡献指南 CONTRIBUTING.md
- 创建安全策略 SECURITY.md

## Impact
- Affected specs: 无直接影响现有功能
- Affected code: 新增 `.github/` 目录及相关配置文件

## ADDED Requirements

### Requirement: GitHub Actions CI 工作流
系统 SHALL 提供 GitHub Actions 持续集成工作流，在每次 Push 和 Pull Request 时自动执行构建和测试。

#### Scenario: Push 到主分支触发 CI
- **WHEN** 开发者推送代码到 main/master 分支
- **THEN** 自动触发 CI 工作流，执行构建、测试和 Lint 检查

#### Scenario: Pull Request 触发 CI
- **WHEN** 创建或更新 Pull Request
- **THEN** 自动触发 CI 工作流，确保代码质量

### Requirement: Issue 模板
系统 SHALL 提供标准化的 Issue 模板，帮助用户和开发者高效报告问题或请求新功能。

#### Scenario: 报告 Bug
- **WHEN** 用户创建 Bug 报告 Issue
- **THEN** 使用预定义模板，包含复现步骤、预期行为、实际行为等字段

#### Scenario: 请求新功能
- **WHEN** 用户创建功能请求 Issue
- **THEN** 使用预定义模板，包含功能描述、使用场景等字段

### Requirement: Pull Request 模板
系统 SHALL 提供 PR 模板，规范代码审查流程。

#### Scenario: 创建 Pull Request
- **WHEN** 开发者创建 Pull Request
- **THEN** 自动填充模板，包含变更描述、测试说明、检查清单等

### Requirement: Dependabot 配置
系统 SHALL 配置 Dependabot 自动检测和更新依赖。

#### Scenario: 依赖更新检测
- **WHEN** 有新的依赖版本发布
- **THEN** Dependabot 自动创建 PR 更新依赖

### Requirement: 贡献指南
系统 SHALL 提供贡献指南，帮助新贡献者了解项目规范。

#### Scenario: 新贡献者参与项目
- **WHEN** 新贡献者想要参与项目
- **THEN** 可通过 CONTRIBUTING.md 了解开发流程、代码规范等

### Requirement: 安全策略
系统 SHALL 提供安全策略文档，指导安全问题的报告流程。

#### Scenario: 发现安全漏洞
- **WHEN** 研究者发现安全漏洞
- **THEN** 可通过 SECURITY.md 了解如何负责任地报告问题
