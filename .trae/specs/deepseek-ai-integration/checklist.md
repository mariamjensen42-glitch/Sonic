# Checklist

## API 基础设施
- [x] DeepSeekApiService 正确实现 API 调用
- [x] API 错误处理完善（网络错误、配额用尽等）
- [x] AiRepository 正确封装 AI 功能

## API Key 配置
- [x] API Key 安全存储（使用 DataStore）
- [x] 设置页面有 API Key 配置入口
- [x] 未配置 API Key 时有友好提示

## AI 信息展示
- [x] AiInfoCard 组件可复用
- [x] 加载状态正确显示
- [x] 错误状态正确显示
- [x] AI 信息内容正确渲染

## 详情页集成
- [x] 专辑详情页有 AI 介绍入口
- [x] 艺术家详情页有 AI 介绍入口
- [x] 各入口点击后正确调用 AI 功能
