package nlpr.cip;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import java.awt.*;
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
    private final List<File> pdfFiles = new ArrayList<>();
    private String selectedDir = "";

    private static final String[] COLUMNS = {"✓", "序号", "原文件名", "新文件名", "状态"};

    public GuiMain() {
        setTitle("PDF Auto Rename Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 620);
        setMinimumSize(new Dimension(740, 400));
        setLocationRelativeTo(null);
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        initUI();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        dirField = new JTextField();
        dirField.setEditable(false);
        dirField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        selectBtn = new JButton("选择文件夹");
        selectBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        selectBtn.setPreferredSize(new Dimension(130, 32));
        topPanel.add(dirField, BorderLayout.CENTER);
        topPanel.add(selectBtn, BorderLayout.EAST);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                String status = (String) getValueAt(row, 4);
                if ("已重命名".equals(status) || "跳过".equals(status) || "失败".equals(status)) {
                    return false;
                }
                return column == 0;
            }
        };
        table = new JTable(tableModel);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(280);
        table.getColumnModel().getColumn(3).setPreferredWidth(280);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(100);

        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = (String) t.getModel().getValueAt(row, 4);
                    if ("已重命名".equals(status)) {
                        c.setForeground(new Color(39, 174, 96));
                    } else if ("失败".equals(status) || "跳过".equals(status)) {
                        c.setForeground(new Color(231, 76, 60));
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        selectAllBtn = new JButton("全选");
        selectAllBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        selectAllBtn.setPreferredSize(new Dimension(80, 32));
        selectAllBtn.setEnabled(false);
        deselectAllBtn = new JButton("全不选");
        deselectAllBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        deselectAllBtn.setPreferredSize(new Dimension(80, 32));
        deselectAllBtn.setEnabled(false);
        scanBtn = new JButton("扫描 PDF");
        scanBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        scanBtn.setPreferredSize(new Dimension(120, 34));
        scanBtn.setEnabled(false);
        renameBtn = new JButton("确认重命名");
        renameBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        renameBtn.setPreferredSize(new Dimension(130, 34));
        renameBtn.setEnabled(false);
        btnPanel.add(selectAllBtn);
        btnPanel.add(deselectAllBtn);
        btnPanel.add(Box.createHorizontalStrut(20));
        btnPanel.add(scanBtn);
        btnPanel.add(renameBtn);

        statusLabel = new JLabel("请先选择包含 PDF 的文件夹");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);

        bottomPanel.add(btnPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        selectBtn.addActionListener(e -> selectFolder());
        scanBtn.addActionListener(e -> scanPDFs());
        renameBtn.addActionListener(e -> doRename());
        selectAllBtn.addActionListener(e -> toggleAll(true));
        deselectAllBtn.addActionListener(e -> toggleAll(false));
    }

    private void toggleAll(boolean selected) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, 4);
            if ("待重命名".equals(status)) {
                tableModel.setValueAt(selected, i, 0);
            }
        }
        updateStatusCount();
    }

    private void updateStatusCount() {
        int total = tableModel.getRowCount();
        int checked = 0;
        for (int i = 0; i < total; i++) {
            Boolean sel = (Boolean) tableModel.getValueAt(i, 0);
            if (sel != null && sel) checked++;
        }
        statusLabel.setText("共 " + total + " 个 PDF，已勾选 " + checked + " 个");
        statusLabel.setForeground(new Color(52, 152, 219));
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择 PDF 所在文件夹");
        if (!selectedDir.isEmpty()) {
            chooser.setCurrentDirectory(new File(selectedDir));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            selectedDir = dir.getAbsolutePath();
            dirField.setText(selectedDir);
            scanBtn.setEnabled(true);
            tableModel.setRowCount(0);
            pdfFiles.clear();
            renameBtn.setEnabled(false);
            selectAllBtn.setEnabled(false);
            deselectAllBtn.setEnabled(false);
            statusLabel.setText("已选择文件夹，点击「扫描 PDF」开始");
            statusLabel.setForeground(Color.GRAY);
        }
    }

    private void scanPDFs() {
        if (selectedDir.isEmpty()) return;

        tableModel.setRowCount(0);
        pdfFiles.clear();
        renameBtn.setEnabled(false);
        selectAllBtn.setEnabled(false);
        deselectAllBtn.setEnabled(false);
        scanBtn.setEnabled(false);
        selectBtn.setEnabled(false);
        statusLabel.setText("正在扫描...");
        statusLabel.setForeground(Color.GRAY);

        new SwingWorker<Void, Object[]>() {
            List<File> found = new ArrayList<>();

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
                        if (title != null && !title.replaceAll(" ", "").isEmpty() && title.length() < 250) {
                            newName = Main.clean_file_name(title) + ".pdf";
                            status = "待重命名";
                            checked = true;
                        } else {
                            newName = "(无法提取标题)";
                            status = "跳过";
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
                scanBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                if (pdfFiles.isEmpty()) {
                    statusLabel.setText("该文件夹下没有找到 PDF 文件");
                    statusLabel.setForeground(new Color(231, 76, 60));
                } else {
                    renameBtn.setEnabled(true);
                    selectAllBtn.setEnabled(true);
                    deselectAllBtn.setEnabled(true);
                    updateStatusCount();
                }
            }
        }.execute();
    }

    private void doRename() {
        int count = tableModel.getRowCount();
        if (count == 0) return;

        List<Integer> pendingRows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Boolean checked = (Boolean) tableModel.getValueAt(i, 0);
            String status = (String) tableModel.getValueAt(i, 4);
            if (checked != null && checked && "待重命名".equals(status)) {
                pendingRows.add(i);
            }
        }

        if (pendingRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有勾选需要重命名的文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "即将重命名 " + pendingRows.size() + " 个文件，是否继续？",
                "确认重命名", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        renameBtn.setEnabled(false);
        scanBtn.setEnabled(false);
        selectBtn.setEnabled(false);
        selectAllBtn.setEnabled(false);
        deselectAllBtn.setEnabled(false);
        statusLabel.setText("正在重命名...");
        statusLabel.setForeground(Color.GRAY);

        new SwingWorker<Void, int[]>() {
            int successCount = 0;
            int failCount = 0;

            @Override
            protected Void doInBackground() {
                for (int i : pendingRows) {
                    String originalPath = pdfFiles.get(i).getAbsolutePath();
                    String newName = (String) tableModel.getValueAt(i, 3);
                    try {
                        Main.change_one_file_name(originalPath, newName);
                        successCount++;
                        publish(new int[]{i, 1});
                    } catch (Exception ex) {
                        failCount++;
                        publish(new int[]{i, 0});
                    }
                }
                return null;
            }

            @Override
            protected void process(List<int[]> chunks) {
                for (int[] update : chunks) {
                    int row = update[0];
                    tableModel.setValueAt(false, row, 0);
                    tableModel.setValueAt(update[1] == 1 ? "已重命名" : "失败", row, 4);
                }
            }

            @Override
            protected void done() {
                scanBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                String msg = String.format("重命名完成：%d 成功，%d 失败", successCount, failCount);
                statusLabel.setText(msg);
                statusLabel.setForeground(failCount > 0 ? new Color(231, 76, 60) : new Color(39, 174, 96));
                JOptionPane.showMessageDialog(GuiMain.this, msg, "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new GuiMain().setVisible(true));
    }
}
