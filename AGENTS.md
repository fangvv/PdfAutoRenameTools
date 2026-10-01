# AGENTS.md — 开发指南

面向后续在本仓库工作的 AI 编码代理与开发者。修改构建方式、依赖或核心约定后，请同步更新本文件。

## 项目简介

PDF 批量重命名工具：解析 PDF 首页版式（取最大字号文本）提取论文标题，并把文件重命名为该标题。提供两种入口：

- GUI：`nlpr.cip.GuiMain`（Swing）
- CLI：`nlpr.cip.Main`（`java -jar PdfAutoRenameTools.jar <目录>`）

## 源码结构

- `src/nlpr/cip/`（唯一源码根，参与构建）：
  - `Main.java` — CLI 入口；改名/文件名清理等核心静态方法，GUI 复用
  - `GuiMain.java` — Swing GUI
  - `TextLocationExtender.java` — `PDFTextStripper` 子类，标题提取核心
  - `Pair.java` — (文本, 字号, X, Y) 数据结构
  - `utils.java` — 目录递归遍历
- `nlpr/cip/GetPaperName.java`（仓库根下）— 早期参考实现，**不在源码根内、不参与构建**，改动 `Main.java` 核心逻辑时建议顺手同步
- `META-INF/MANIFEST.MF` — IDEA artifact 使用的清单（`Main-Class: nlpr.cip.Main`）
- `.idea/artifacts/PdfAutoRenameTools_jar.xml` — IDEA fat jar 配置（只产出 CLI 版）

## 依赖（仓库根目录的 jar，直接挂 classpath）

| jar | 版本 |
|---|---|
| `pdfbox-3.0.8.jar` | 3.0.8 |
| `fontbox-3.0.8.jar` | 3.0.8 |
| `pdfbox-io-3.0.8.jar` | 3.0.8（PDFBox 3 新增的必需模块） |
| `commons-logging-1.4.0.jar` | 1.4.0（PDFBox 3 仍依赖 commons-logging，不是 log4j） |

禁忌：

- **不要引入 `pdfbox-app-*.jar`**：内嵌 bouncycastle/picocli 等，fat jar 体积翻倍且用不上
- **不要引入 `fontbox-3.0.0-RC1.jar`**：历史遗留的版本错配，与 pdfbox 2.x/3.x API 不兼容，已移除
- PDFBox 3 API：加载用 `org.apache.pdfbox.Loader.loadPDF(File)`，`PDDocument.load()` 已删除；`PDType1Font` 静态常量改为 `new PDType1Font(Standard14Fonts.FontName.XXX)`
- 静音 PDFBox 日志靠 JUL：`Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF)`（经 commons-logging→JUL 桥接生效）

## 编译

```bash
# Windows classpath 用分号分隔；源码含中文，必须 -encoding UTF-8
CP="pdfbox-3.0.8.jar;fontbox-3.0.8.jar;pdfbox-io-3.0.8.jar;commons-logging-1.4.0.jar"
javac --release 21 -encoding UTF-8 -cp "$CP" -d out/production/PdfAutoRenameTools src/nlpr/cip/*.java
```

产物字节码固定按 `--release 21`（运行环境 Java 21+），不要用裸 JDK 目标版本编译发布产物。

## 打包 fat jar（可分发产物）

产物固定输出到 `out/artifacts/PdfAutoRenameTools_jar/`：

- `PdfAutoRenameTools.jar`（`Main-Class: nlpr.cip.Main`）
- `PdfAutoRenameTools_GUI.jar`（`Main-Class: nlpr.cip.GuiMain`）

```bash
# 1) 按上文编译后，组装 staging：classes + 四个依赖 jar（排除各自的 META-INF/MANIFEST.MF）
rm -rf build_stage && mkdir -p build_stage
cp -r out/production/PdfAutoRenameTools/. build_stage/
for j in pdfbox-3.0.8.jar fontbox-3.0.8.jar pdfbox-io-3.0.8.jar commons-logging-1.4.0.jar; do
  unzip -o -q "$j" -d build_stage -x 'META-INF/MANIFEST.MF'
done
rm -f build_stage/META-INF/MANIFEST.MF

# 2) 生成两个可执行 fat jar（jar 位于 JDK bin/，Windows 下为 %JAVA_HOME%\bin\jar.exe）
JAR="$JAVA_HOME/bin/jar"
"$JAR" --create --file out/artifacts/PdfAutoRenameTools_jar/PdfAutoRenameTools.jar     --main-class nlpr.cip.Main   -C build_stage .
"$JAR" --create --file out/artifacts/PdfAutoRenameTools_jar/PdfAutoRenameTools_GUI.jar --main-class nlpr.cip.GuiMain -C build_stage .

# 3) 清理临时目录
rm -rf build_stage out/production
```

注意：**IDEA 的 Build Artifacts 只产出 CLI 版**；GUI 版必须用上面的命令重建（或先在 `.idea/artifacts/` 增加对应 artifact）。

## 冒烟测试（发布前必做）

1. 用临时 `MakePdf.java`（PDFBox 3 API）生成一个标题为大字号 Helvetica-Bold（如 `My Great Paper Title`）、正文小字号的 `test.pdf`
2. `java -jar out/.../PdfAutoRenameTools.jar <该目录>` → 应改名为 `My Great Paper Title.pdf`
3. 对同一路径再跑一次 CLI → 应**无任何改名动作**（同名跳过守卫生效，不产生 `_1` 垃圾文件）
4. GUI jar 启动应出现窗口且长时间不退出（无栈输出即视为通过）
5. 测试完删除所有临时产物（staging、MakePdf、测试 PDF）

## 代码约定

### GUI（GuiMain.java）

- 行状态是字符串列 `COL_STATUS`，合法值仅限常量：`ST_PENDING`(待重命名) / `ST_RENAMED`(已重命名) / `ST_SKIPPED`(跳过) / `ST_FAILED`(失败) / `ST_UNCHANGED`(无需修改)。**必须引用常量，禁止写字符串字面量**
- 提取到的新名与原文件名相同（忽略大小写）→ `ST_UNCHANGED`：默认不勾选、勾选框不可编辑、全选/重命名均不触碰它
- 只有 `COL_CHECK`（第 0 列）可编辑，且仅 `ST_PENDING` 行可勾选
- `SwingWorker` 后台线程**禁止读写 `TableModel`**：需要的表格数据先 EDT 上快照（见 `doRename()` 的 rows/paths/targets）
- 自定义渲染器收到的是视图行号，读模型数据前先 `convertRowIndexToModel()`

### 改名核心（Main.java）

- `change_one_file_name(original, target)`：`target` 为**含扩展名的完整文件名**；冲突时在扩展名前加 `_1.._10`；返回**实际落盘文件名**，失败返回 `null`（GUI 依据返回值定状态并回写新文件名列）。修改此函数须保持契约
- 新名与原文件名相同（忽略大小写）时直接跳过，不调用 `renameTo`（同名 `renameTo` 在 Windows 上可能失败并触发 `_1` 连锁改名）
- `clean_file_name()` 负责 Windows 文件名合法性：非法字符替换为 `_`、去控制字符、压缩空白、去首尾空白/点
- 标题有效性判定：非空（去空格）、长度 < 250（GUI 侧 `isRenameable()`）

## 文档同步规则

- **`README.md`（中文）与 `README_EN.md`（英文）必须同步修改**：功能说明、依赖表、状态颜色说明、运行要求等，任何一侧变更都要同步另一侧
- 构建/依赖/打包方式变化时，更新本文件

## 运行

- Java 21 及以上
- GUI：`java -jar PdfAutoRenameTools_GUI.jar`
- CLI：`java -jar PdfAutoRenameTools.jar <目录>`（递归处理该目录下全部 PDF）
