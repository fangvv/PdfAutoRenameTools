# PdfAutoRenameTools

English README | [中文说明](README.md)

> **Are you still renaming downloaded research paper PDFs one by one by hand?**

When you download papers in bulk from academic search engines, the resulting files often have uninformative names such as `paper.pdf`, `document(1).pdf`, or `2401.08392.pdf`. To organize them manually, you usually need to open each PDF, find the title, close the file, and rename it. Even if one paper only takes about ten seconds, organizing hundreds of papers can easily consume an entire afternoon.

**PdfAutoRenameTools solves this problem with one automated workflow.**

The tool reads layout information from the first page of each PDF, identifies the paper title, and renames the file to that title. It supports batch processing without manual intervention, making literature organization much faster and more convenient.

## Key Features

- **Intelligent title detection** — Uses PDF text font size and coordinate information to locate the largest prominent text on the first page as the paper title.
- **Recursive batch processing** — Scans an entire folder and its subfolders to process all PDF files in one run.
- **GUI and command-line modes** — Provides both a graphical user interface and a command-line interface for different usage scenarios.
- **Parallel processing** — Uses Java parallel streams to improve performance when processing large collections of papers.
- **Safer renaming workflow** — In GUI mode, you can preview the proposed rename results before confirming the operation, reducing the risk of accidental changes. PDFs whose file names already match the detected title are marked as unchanged and remain unchecked by default.

## Background

The core algorithm of this project is based on the original public version shared by Mr. Bingning Wang on [his homepage](https://bingning.wang/research/Article/?id=114). The original tool has been very useful in daily research work. During long-term use, one known issue was found: for some papers, the extracted title only included the first line, which caused incomplete renamed file names.

To fix this issue, the original JAR was reverse-analyzed with [Luyten](https://github.com/deathmarine/Luyten). After understanding the original source logic, the title extraction algorithm was specifically optimized, and a Swing-based graphical user interface was added to make the tool easier to use.

If Mr. Bingning Wang believes that publishing this code is inappropriate, the repository owner may be contacted to remove it.

## Installation and Requirements

### Runtime Requirement

A Java runtime environment (Java 21 or newer) is required to run the generated JAR files, which are compiled with `--release 21`. The project documentation records the following Java environment as the compilation environment:

```text
java version "26.0.1" 2026-04-21
Java(TM) SE Runtime Environment (build 26.0.1+8-34)
Java HotSpot(TM) 64-Bit Server VM (build 26.0.1+8-34, mixed mode, sharing)
```

Other Java versions may also work, but you should configure a suitable Java environment for your system.

### JAR Location

The generated JAR files are located in:

```text
out\artifacts\PdfAutoRenameTools_jar\
```

This directory contains the GUI and command-line JAR files:

```text
PdfAutoRenameTools_GUI.jar
PdfAutoRenameTools.jar
```

## Usage

### Option 1: Graphical User Interface, recommended

Run the GUI version with:

```bash
java -jar PdfAutoRenameTools_GUI.jar
```

After the graphical interface opens:

1. Click **Select Folder** to choose the directory that contains PDF files.
2. Click **Scan PDF** to automatically detect the paper title for each PDF. The table will display each mapping from the original file name to the proposed new file name.
3. After confirming that the preview results are correct, click **Confirm Rename** to perform the batch rename operation.

The table uses colors to show status:

- Green: the file has been renamed successfully.
- Red: the file failed or was skipped.
- Gray: no change needed (the file name already matches the detected title; unchecked by default).

### Option 2: Command Line

Run the command-line version with a target directory path:

```bash
java -jar PdfAutoRenameTools.jar <directory-name>
```

The program recursively scans all PDF files under the specified directory and renames them according to the detected title from the first page.

Example:

```bash
java -jar PdfAutoRenameTools.jar D:\Papers
```

## How It Works

### Core Algorithm

The title detection logic is implemented in `TextLocationExtender.java`:

1. The class extends PDFBox `PDFTextStripper` and captures each text fragment during parsing, including its content, font size, X coordinate, and Y coordinate.
2. All font sizes are sorted in descending order. The font size at the first one-third position is used as a threshold to filter out smaller body text.
3. From the remaining text, the algorithm selects continuous text lines that are near the top of the page, based on the smallest Y coordinate, and share a consistent font size. These lines are then joined as the title.
4. The extracted title is cleaned before being used as a file name. This includes removing hyphenated line breaks and replacing characters that are illegal in file names.

## Source Code Structure

```text
src/nlpr/cip/
├── Main.java                  # Command-line entry point
├── GuiMain.java               # Swing graphical user interface
├── TextLocationExtender.java  # Core PDF text parsing and title extraction logic
├── Pair.java                  # Data structure: text, font size, X coordinate, Y coordinate
└── utils.java                 # Recursive file and directory traversal utilities
```

## Dependencies

| Library | Version | Purpose |
| --- | --- | --- |
| [Apache PDFBox](https://pdfbox.apache.org/) | 3.0.8 | PDF parsing and text extraction |
| [FontBox](https://pdfbox.apache.org/) | 3.0.8 | PDF font information handling |
| [PDFBox IO](https://pdfbox.apache.org/) | 3.0.8 | Random-access IO module required by PDFBox 3 |
| [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/) | 1.4.0 | Logging framework |

The repository also includes the corresponding JAR dependencies such as `pdfbox-3.0.8.jar`, `fontbox-3.0.8.jar`, `pdfbox-io-3.0.8.jar`, and `commons-logging-1.4.0.jar`.

## Notes and Limitations

- The title detection method is heuristic. It works by analyzing first-page layout information, especially font size and position.
- Papers with unusual layouts, scanned image-only PDFs, missing embedded text, or non-standard title formatting may not be detected correctly.
- GUI mode is recommended when processing important files because it allows you to preview rename results before applying them.
- Before running batch renaming on a large folder, consider testing the tool on a small sample or keeping a backup of your files.
