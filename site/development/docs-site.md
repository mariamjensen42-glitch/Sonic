# 文档站点

本站由 **[Zensical](https://zensical.org/)** 生成，依赖由 **[uv](https://docs.astral.sh/uv/)** 管理，
产物自动发布到 GitHub Pages。

## 目录约定

```text
Sonic/
├── pyproject.toml          uv 项目定义 + zensical 依赖
├── uv.lock                 锁文件（依赖版本确定）
├── zensical.toml           站点配置：站点信息、导航、主题、Markdown 扩展
├── site/                   文档源文件（Markdown）
│   ├── index.md            首页
│   ├── getting-started.md
│   ├── features/           功能页
│   ├── architecture.md
│   ├── contributing.md     嵌仓库根的 CONTRIBUTING.md
│   ├── changelog.md        嵌仓库根的 CHANGELOG.md
│   ├── development/
│   └── assets/images/      logo / favicon
└── site/build/             构建产物（gitignore，不入库）
```

## 本地预览

```bash
# 首次会创建 .venv 并装依赖
uv sync

# 起本地服务器，改 Markdown 会自动重建
uv run zensical serve
# → http://localhost:8000
```

## 构建

```bash
uv run zensical build --clean
# 产物在 site/build/
```

`--clean` 会先清空输出目录，避免上一次构建的残留文件混进去。

## 更新依赖

```bash
# 升级 zensical 到最新版
uv update --dev

# 重新锁定（CI 用 --frozen 跑，不会自动改锁文件）
uv lock
```

!!! warning "不要用 uv 的 symlink 模式"
    Zensical 的主题资源是打包在 wheel 里的，symlink 链接模式会破坏它，
    官方明确说明暂不支持。`pyproject.toml` 里已经把 `link-mode` 固定成 `copy`，
    不要改回 `symlink`。

## 发布到 GitHub Pages

推送 `master` 分支即触发 `.github/workflows/docs.yml`：

1. `astral-sh/setup-uv` 装 uv
2. `uv run --frozen zensical build --clean` 构建
3. `actions/upload-pages-artifact` 上传 `site/build`
4. `actions/deploy-pages` 发布

站点地址：<https://mariamjensen42-glitch.github.io/Sonic/>

### 首次启用需要做的

仓库设置里把 **Settings → Pages → Build and deployment → Source** 设为
**GitHub Actions**。之后每次推 `master` 都会自动重新发布。

## 加一页新文档

1. 在 `site/` 下新建 `.md`
2. 在 `zensical.toml` 的 `nav` 里加一条
3. 提交，推送

`nav` 里没登记的文件会被 zensical 提示（`not_in_nav`）。写完记得同步 `nav`。

## 写文档的小约定

- 提示框用 Material 的 admonition：`!!! tip "标题"` / `!!! warning` / `!!! note`
- 卡片用 `grid cards`：

    ```markdown
    <div class="grid cards" markdown>

    -   :material-rocket: &nbsp; **标题**

        ---

        描述文字

    </div>
    ```

- 图标用内置的 material / lucide 短码：`:material-rocket:`、`:lucide/sun:`
- 代码块标语言（`kotlin` / `bash` / `toml`），高亮才生效
- 站内链接用相对路径的 `.md` 源文件形式（zensical 构建时会改写成站点 URL）
