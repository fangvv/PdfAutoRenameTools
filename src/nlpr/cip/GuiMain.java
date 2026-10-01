package nlpr.cip;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

public class GuiMain extends JFrame {

    private JTextField dirField;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton selectBtn, scanBtn, renameBtn, selectAllBtn, deselectAllBtn;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private final List<File> pdfFiles = new ArrayList<>();
    private String selectedDir = "";

    private static final String[] COLUMNS = {"✓", "序号", "原文件名", "新文件名", "状态"};
    private static final int COL_CHECK = 0;
    private static final int COL_INDEX = 1;
    private static final int COL_ORIG = 2;
    private static final int COL_NEW = 3;
    private static final int COL_STATUS = 4;

    private static final String ST_PENDING = "待重命名";
    private static final String ST_RENAMED = "已重命名";
    private static final String ST_SKIPPED = "跳过";
    private static final String ST_FAILED = "失败";
    private static final String ST_UNCHANGED = "无需修改";

    private static final Color BG = new Color(0xF5F6F8);
    private static final Color PANEL = Color.WHITE;
    private static final Color ACCENT = new Color(0x2F6FED);
    private static final Color OK = new Color(0x1E8E3E);
    private static final Color ERR = new Color(0xD93025);
    private static final Color TEXT = new Color(0x202124);
    private static final Color MUTED = new Color(0x5F6368);
    private static final Color STRIPE = new Color(0xF1F3F4);
    private static final Color BORDER = new Color(0xDADCE0);
    private static final Color HEADER_BG = new Color(0xE8EAED);
    private static final Color SELECTION = new Color(0xD2E3FC);

    public GuiMain() {
        setTitle("PDF Auto Rename Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(820, 480));
        setLocationRelativeTo(null);
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        getContentPane().setBackground(BG);
        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildTableArea(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        selectBtn.addActionListener(e -> selectFolder());
        scanBtn.addActionListener(e -> scanPDFs());
        renameBtn.addActionListener(e -> doRename());
        selectAllBtn.addActionListener(e -> toggleAll(true));
        deselectAllBtn.addActionListener(e -> toggleAll(false));
        dirField.addActionListener(e -> commitTypedDir());
    }

    private JPanel buildHeader() {
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BG);
        JLabel title = new JLabel("PDF 批量重命名");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("根据 PDF 标题自动重命名文件名");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(MUTED);
        JPanel sub = new JPanel(new BorderLayout());
        sub.setOpaque(false);
        sub.add(subtitle, BorderLayout.SOUTH);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(title, BorderLayout.NORTH);
        left.add(sub, BorderLayout.CENTER);
        titleRow.add(left, BorderLayout.WEST);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JPanel folderRow = new JPanel(new BorderLayout(12, 0));
        folderRow.setBackground(BG);
        dirField = new JTextField();
        dirField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        dirField.setForeground(TEXT);
        dirField.setMargin(new Insets(9, 11, 9, 11));
        dirField.setToolTipText("点击「选择文件夹」或直接粘贴路径后回车");
        dirField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        selectBtn = makeButton("选择文件夹", ACCENT, Color.WHITE);
        folderRow.add(dirField, BorderLayout.CENTER);
        folderRow.add(selectBtn, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        header.add(titleRow, BorderLayout.NORTH);
        header.add(folderRow, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane buildTableArea() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_CHECK) return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                String status = (String) getValueAt(row, COL_STATUS);
                if (ST_RENAMED.equals(status) || ST_SKIPPED.equals(status) || ST_FAILED.equals(status)
                        || ST_UNCHANGED.equals(status)) {
                    return false;
                }
                return column == COL_CHECK;
            }
        };
        table = new JTable(tableModel);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.getTableHeader().setReorderingAllowed(false);
        styleHeader();

        table.getColumnModel().getColumn(COL_CHECK).setPreferredWidth(34);
        table.getColumnModel().getColumn(COL_CHECK).setMaxWidth(44);
        table.getColumnModel().getColumn(COL_INDEX).setPreferredWidth(48);
        table.getColumnModel().getColumn(COL_INDEX).setMaxWidth(60);
        table.getColumnModel().getColumn(COL_ORIG).setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_NEW).setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        table.getColumnModel().getColumn(COL_STATUS).setMaxWidth(120);

        table.setDefaultRenderer(String.class, new StatusRenderer());
        JCheckBox headerBox = (JCheckBox) table.getDefaultRenderer(Boolean.class);
        if (headerBox != null) headerBox.setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        scrollPane.getViewport().setBackground(PANEL);
        scrollPane.setBackground(PANEL);
        return scrollPane;
    }

    private void styleHeader() {
        JTableHeader h = table.getTableHeader();
        h.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        h.setBackground(HEADER_BG);
        h.setForeground(TEXT);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                DefaultTableCellRenderer c = (DefaultTableCellRenderer) super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                c.setBackground(HEADER_BG);
                c.setForeground(TEXT);
                c.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                if (column == COL_CHECK || column == COL_INDEX || column == COL_STATUS) {
                    c.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    c.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });
    }

    private JPanel buildFooter() {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);
        selectAllBtn = makeButton("全选", new Color(0x5F6368), Color.WHITE);
        deselectAllBtn = makeButton("全不选", new Color(0x5F6368), Color.WHITE);
        scanBtn = makeButton("扫描 PDF", ACCENT, Color.WHITE);
        renameBtn = makeButton("确认重命名", OK, Color.WHITE);
        renameBtn.setFont(renameBtn.getFont().deriveFont(Font.BOLD));
        selectAllBtn.setEnabled(false);
        deselectAllBtn.setEnabled(false);
        scanBtn.setEnabled(false);
        renameBtn.setEnabled(false);
        btnPanel.add(selectAllBtn);
        btnPanel.add(deselectAllBtn);
        btnPanel.add(Box.createHorizontalStrut(40));
        btnPanel.add(scanBtn);
        btnPanel.add(renameBtn);

        statusLabel = new JLabel("请先选择包含 PDF 的文件夹");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT);
        progressBar.setBorder(BorderFactory.createLineBorder(BORDER));
        progressBar.setVisible(false);

        JPanel statusBar = new JPanel(new BorderLayout(12, 0));
        statusBar.setOpaque(false);
        statusBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(btnPanel, BorderLayout.NORTH);
        footer.add(statusBar, BorderLayout.SOUTH);
        return footer;
    }

    // ---------- 扁平按钮 ----------
    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        b.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 24));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("base", bg);
        MouseListener ml = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { refreshButton(b); }
            @Override public void mouseExited(MouseEvent e) { refreshButton(b); }
            @Override public void mousePressed(MouseEvent e) { refreshButton(b); }
            @Override public void mouseReleased(MouseEvent e) { refreshButton(b); }
        };
        b.addMouseListener(ml);
        b.addPropertyChangeListener("enabled", e -> refreshButton(b));
        return b;
    }

    private void refreshButton(JButton b) {
        Color base = (Color) b.getClientProperty("base");
        if (base == null) base = b.getBackground();
        Color c;
        if (!b.isEnabled()) {
            c = blend(base, Color.WHITE, 0.55f);
        } else if (b.getModel().isArmed()) {
            c = blend(base, Color.BLACK, 0.18f);
        } else if (b.getModel().isRollover()) {
            c = blend(base, Color.WHITE, 0.12f);
        } else {
            c = base;
        }
        b.setBackground(c);
    }

    private static Color blend(Color a, Color b, float ratio) {
        float ia = 1 - ratio;
        return new Color(
                Math.round(a.getRed() * ia + b.getRed() * ratio),
                Math.round(a.getGreen() * ia + b.getGreen() * ratio),
                Math.round(a.getBlue() * ia + b.getBlue() * ratio));
    }

    // ---------- 渲染器：斑马纹 + 状态色 ----------
    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            int modelRow = t.convertRowIndexToModel(row);
            String status = (String) t.getModel().getValueAt(modelRow, COL_STATUS);
            if (!isSelected) {
                c.setBackground(modelRow % 2 == 0 ? PANEL : STRIPE);
                if (ST_RENAMED.equals(status)) {
                    c.setForeground(OK);
                } else if (ST_FAILED.equals(status) || ST_SKIPPED.equals(status)) {
                    c.setForeground(ERR);
                } else if (ST_UNCHANGED.equals(status)) {
                    c.setForeground(MUTED);
                } else {
                    c.setForeground(TEXT);
                }
            } else {
                c.setForeground(t.getSelectionForeground());
            }
            if (column == COL_INDEX || column == COL_STATUS) {
                ((DefaultTableCellRenderer) c).setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                ((DefaultTableCellRenderer) c).setHorizontalAlignment(SwingConstants.LEFT);
            }
            ((DefaultTableCellRenderer) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    // ---------- 业务 ----------
    private void toggleAll(boolean selected) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, COL_STATUS);
            if (ST_PENDING.equals(status)) {
                tableModel.setValueAt(selected, i, COL_CHECK);
            }
        }
        updateStatusCount();
    }

    private void updateStatusCount() {
        int total = tableModel.getRowCount();
        int checked = 0;
        for (int i = 0; i < total; i++) {
            Boolean sel = (Boolean) tableModel.getValueAt(i, COL_CHECK);
            if (sel != null && sel) checked++;
        }
        statusLabel.setText("共 " + total + " 个 PDF，已勾选 " + checked + " 个");
        statusLabel.setForeground(ACCENT);
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择 PDF 所在文件夹");
        if (!selectedDir.isEmpty()) {
            chooser.setCurrentDirectory(new File(selectedDir));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectDir(chooser.getSelectedFile());
        }
    }

    private void commitTypedDir() {
        String path = dirField.getText().trim();
        File dir = new File(path);
        if (path.isEmpty()) {
            setStatus("请输入文件夹路径", MUTED);
            return;
        }
        if (!dir.isDirectory()) {
            setStatus("路径无效，请选择一个存在的文件夹", ERR);
            JOptionPane.showMessageDialog(this, "该路径不是一个存在的文件夹", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectDir(dir);
    }

    private void selectDir(File dir) {
        selectedDir = dir.getAbsolutePath();
        dirField.setText(selectedDir);
        tableModel.setRowCount(0);
        pdfFiles.clear();
        setWorking(false);
        setStatus("已选择文件夹，点击「扫描 PDF」开始", MUTED);
    }

    private void scanPDFs() {
        if (selectedDir.isEmpty()) return;

        tableModel.setRowCount(0);
        pdfFiles.clear();
        setWorking(true);
        setStatus("正在扫描...", MUTED);
        progressBar.setIndeterminate(true);

        new SwingWorker<Void, Object[]>() {
            final List<File> found = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                List<String> files = utils.GetDirFiles(selectedDir);
                for (String f : files) {
                    if (f.toLowerCase().endsWith(".pdf")) {
                        File pdfFile = new File(f);
                        found.add(pdfFile);
                        String title = Main.GetTitleOfPDF(pdfFile.getAbsolutePath());
                        String newName;
                        String status;
                        boolean checked;
                        if (isRenameable(title)) {
                            newName = title + ".pdf";
                            if (pdfFile.getName().equalsIgnoreCase(newName)) {
                                // 文件名已与标题一致，无需再改，默认不勾选
                                status = ST_UNCHANGED;
                                checked = false;
                            } else {
                                status = ST_PENDING;
                                checked = true;
                            }
                        } else {
                            newName = "(无法提取标题)";
                            status = ST_SKIPPED;
                            checked = false;
                        }
                        publish(new Object[]{checked, pdfFile.getName(), newName, status});
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    int idx = tableModel.getRowCount() + 1;
                    tableModel.addRow(new Object[]{row[0], String.valueOf(idx), row[1], row[2], row[3]});
                }
            }

            @Override
            protected void done() {
                pdfFiles.addAll(found);
                setWorking(false);
                if (pdfFiles.isEmpty()) {
                    setStatus("该文件夹下没有找到 PDF 文件", ERR);
                } else {
                    updateStatusCount();
                }
            }
        }.execute();
    }

    private static boolean isRenameable(String title) {
        return title != null && !title.replace(" ", "").isEmpty() && title.length() < 250;
    }

    private void doRename() {
        int count = tableModel.getRowCount();
        if (count == 0) return;

        final List<Integer> rows = new ArrayList<>();
        final List<String> paths = new ArrayList<>();
        final List<String> targets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Boolean checked = (Boolean) tableModel.getValueAt(i, COL_CHECK);
            String status = (String) tableModel.getValueAt(i, COL_STATUS);
            if (checked != null && checked && ST_PENDING.equals(status)) {
                rows.add(i);
                paths.add(pdfFiles.get(i).getAbsolutePath());
                targets.add((String) tableModel.getValueAt(i, COL_NEW));
            }
        }

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有勾选需要重命名的文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "即将重命名 " + rows.size() + " 个文件，是否继续？",
                "确认重命名", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setWorking(true);
        setStatus("正在重命名...", MUTED);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(rows.size());
        progressBar.setValue(0);
        progressBar.setString(rows.size() + " 个");

        new SwingWorker<Void, Object[]>() {
            int successCount = 0;
            int failCount = 0;

            @Override
            protected Void doInBackground() {
                for (int j = 0; j < paths.size(); j++) {
                    String finalName = Main.change_one_file_name(paths.get(j), targets.get(j));
                    if (finalName != null) {
                        successCount++;
                        publish(new Object[]{rows.get(j), 1, finalName});
                    } else {
                        failCount++;
                        publish(new Object[]{rows.get(j), 0, null});
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] update : chunks) {
                    int row = (Integer) update[0];
                    boolean ok = (Integer) update[1] == 1;
                    if (ok) {
                        tableModel.setValueAt(update[2], row, COL_NEW);
                        tableModel.setValueAt(ST_RENAMED, row, COL_STATUS);
                    } else {
                        tableModel.setValueAt(ST_FAILED, row, COL_STATUS);
                    }
                    tableModel.setValueAt(false, row, COL_CHECK);
                    progressBar.setValue(progressBar.getValue() + 1);
                }
            }

            @Override
            protected void done() {
                setWorking(false);
                String msg = String.format("重命名完成：%d 成功，%d 失败", successCount, failCount);
                setStatus(msg, failCount > 0 ? ERR : OK);
                JOptionPane.showMessageDialog(GuiMain.this, msg, "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // ---------- 状态/忙碌辅助 ----------
    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void setWorking(boolean working) {
        progressBar.setVisible(working);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        scanBtn.setEnabled(!working && !selectedDir.isEmpty());
        selectBtn.setEnabled(!working);
        renameBtn.setEnabled(!working && !pdfFiles.isEmpty());
        selectAllBtn.setEnabled(!working && !pdfFiles.isEmpty());
        deselectAllBtn.setEnabled(!working && !pdfFiles.isEmpty());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new GuiMain().setVisible(true));
    }
}
