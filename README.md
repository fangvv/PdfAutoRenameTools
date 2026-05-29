# PdfAutoRenameTools

> **你还在手动一篇一篇地给下载的论文 PDF 重命名吗？**

当你从学术搜索引擎批量下载论文时，得到的往往是 `paper.pdf`、`document(1).pdf`、`2401.08392.pdf` 这样毫无信息量的文件名。你需要逐一打开、找到标题、关闭、重命名——一篇论文至少浪费 10 秒钟。如果你有上百篇文献要整理，这件事将吞噬你整整一个下午。

**PdfAutoRenameTools 帮你一键解决这个问题。**

它能够自动读取 PDF 首页的排版信息，精准识别出论文标题，并将文件重命名为标题本身。整个过程无需人工干预，批量处理，瞬间完成。

## 功能特性

- 🔍 **智能标题识别** — 基于 PDF 文本的字号与坐标信息，自动定位首页最大字号文字作为论文标题
- 📂 **递归批量处理** — 支持递归扫描整个文件夹及其子目录，一次处理所有 PDF
- 🖥️ **图形界面 + 命令行** — 提供 GUI 和 CLI 两种使用方式，满足不同场景需求
- ⚡ **并行处理** — 利用 Java 并行流加速，面对大量文献依然高效
- 🛡️ **安全重命名** — GUI 模式下可先预览改名结果，确认后再执行，避免误操作

## 背景

本工具的核心算法源自王炳宁先生公开在[其主页](https://bingning.wang/research/Article/?id=114)上的原始版本。本人在日常科研中长期使用，受益良多。在使用过程中发现原版存在一个已知问题——部分论文的标题只能截取到第一行，导致重命名不完整。

为了修正这一问题，我通过反编译工具 [luyten](https://github.com/deathmarine/Luyten) 对原始 jar 进行了逆向分析，在充分理解源码逻辑后，对标题提取算法做了针对性优化，并在此基础上新增了 GUI 图形界面，使操作体验更加友好。

如王炳宁先生认为代码公开有不妥之处，可随时联系我删除。

## 使用方法

生成的 jar 文件位于 `out\artifacts\PdfAutoRenameTools_jar\` 目录下。

### 方式一：GUI 图形界面（推荐）

```bash
java -jar PdfAutoRenameTools_GUI.jar
```

启动后弹出图形界面：

1. 点击 **「选择文件夹」** — 选择包含 PDF 文件的目录
2. 点击 **「扫描 PDF」** — 自动识别每个 PDF 的论文标题，表格实时显示「原文件名 → 新文件名」
3. 确认无误后点击 **「确认重命名」** — 执行批量重命名

表格中状态以颜色区分：🟢 绿色 = 已重命名，🔴 红色 = 失败或跳过。

### 方式二：命令行

```bash
java -jar PdfAutoRenameTools.jar 目录名
```

程序会递归扫描指定目录下的所有 PDF 文件，根据首页最大字号文字自动重命名。

## 技术实现

### 核心算法

标题识别的核心逻辑位于 `TextLocationExtender.java`：

1. 继承 PDFBox 的 `PDFTextStripper`，在解析过程中捕获每个文本片段的**内容、字号、坐标**
2. 将所有字号降序排列，取第 1/3 位字号作为阈值，过滤掉正文等小字号文本
3. 在剩余文本中，选取 Y 坐标最小（即页面最上方）且字号一致的连续文本行，拼接为标题
4. 对标题进行清理：移除连字符换行、替换文件名非法字符

### 源码结构

```
src/nlpr/cip/
├── Main.java                  # 程序入口（命令行）
├── GuiMain.java               # GUI 图形界面（Swing）
├── TextLocationExtender.java  # PDF 文本解析与标题提取核心
├── Pair.java                  # 数据结构：(文本, 字号, X坐标, Y坐标)
└── utils.java                 # 文件目录递归遍历工具
```

### 依赖库

| 库 | 版本 | 用途 |
|----|------|------|
| [Apache PDFBox](https://pdfbox.apache.org/) | 2.0.23 | PDF 文件解析与文本提取 |
| [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/) | 1.2 | 日志框架 |
| [FontBox](https://pdfbox.apache.org/) | 3.0.0-RC1 | PDF 字体信息处理 |

## 编译环境

```
java version "26.0.1" 2026-04-21
Java(TM) SE Runtime Environment (build 26.0.1+8-34)
Java HotSpot(TM) 64-Bit Server VM (build 26.0.1+8-34, mixed mode, sharing)
```

运行需要 Java 环境，具体版本请自行配置。
