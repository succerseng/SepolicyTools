package cc.succerseng.sepolicytools.gui;

import cc.succerseng.sepolicytools.modifypolicy.contextfiles.ContextsUtils;
import cc.succerseng.sepolicytools.modifypolicy.contextfiles.impl.ContextsUtilsImpl;
import cc.succerseng.sepolicytools.modifypolicy.contextfiles.impl.FileContextsFormatImpl;
import cc.succerseng.sepolicytools.createpolicy.impl.CreateRuleFromLogImpl;
import cc.succerseng.sepolicytools.modifypolicy.generalfiles.SePolicyDirUtils;
import cc.succerseng.sepolicytools.modifypolicy.generalfiles.impl.SePolicyDirUtilsImpl;
import cc.succerseng.sepolicytools.utils.impl.AdbUtilsImpl;
import cc.succerseng.sepolicytools.utils.impl.FilePathUtilsImpl;
import cc.succerseng.sepolicytools.utils.impl.StreamHelperImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 图形界面
 */
public class SePolicyToolsGUI extends JFrame {
    private static final StringBuilder logStringBuilder = new StringBuilder();
    private static JTextArea textArea = null;
    private final SePolicyDirUtils sePolicyDirUtils = new SePolicyDirUtilsImpl();
    private final FileContextsFormatImpl fileContextsFormat = new FileContextsFormatImpl();
    private final ContextsUtils contextsUtils = new ContextsUtilsImpl();
    private final Font globalFont = new Font(null, Font.BOLD, 30);
    private final Font textFont = new Font(null, Font.BOLD, 20);
    private final Font buttonFont = new Font(null, Font.BOLD, 15);
    private final Color themeColor = new Color(195, 233, 242);
    private File sourceFile = new File(".");
    private Container contentPane = null;
    private JLabel sourceDirLabel = null;
    private JTextField sourceDirString = null;
    private JButton selectDirButton = null;
    private JButton clearLogButton = null;
    private JButton autoScrollButton = null;
    private JPanel selectSourcePanel = null;
    private JButton formatTEFilesButton = null;
    private JButton formatFileContextsButton = null;
    private JButton reWriteTEFilesButton = null;
    private JButton logToSePolicyButton = null;
    private JButton formatAllContextsFileButton = null;
    private JScrollPane centerPanel = null;
    private JPanel eastPanel = null;
    private JButton autoRun = null;
    private static boolean autoScrollFlag = true;

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));

    // 标记当前是否有线程在跑
    private volatile boolean hasRun = false;

    Lock lock = new ReentrantLock();

    // 重复操作输出(大概永远都不会触发
    private final String[] info = {"上一个操作执行中...", "啊！~不...不行，正在运行呢", "木大木大", "就是不动，哎嘿~"};

    /**
     * 界面
     */
    public SePolicyToolsGUI() {
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        /*
            北边
        */
        {
            sourceDirLabel = new JLabel("工作路径:   ");
            sourceDirLabel.setFont(textFont);

            sourceDirString = new JTextField(30);
            sourceDirString.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File path = new File(sourceDirString.getText());

                    if (path.exists()) {
                        sourceFile = path;
                        SePolicyToolsGUI.log("切换文件路径" + sourceFile.getAbsolutePath());
                    }

                    updatedSourceDir();
                }
            });
            sourceDirString.setFont(textFont);
            sourceDirString.setText(sourceFile.getAbsolutePath());

            selectDirButton = new JButton("选择工作目录");
            selectDirButton.setBackground(themeColor);
            selectDirButton.setFont(buttonFont);
            selectDirButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File file = null;
                    if ((file = selectDirDialog()) == null) {
                        SePolicyToolsGUI.log("未选择文件");
                    } else {
                        sourceFile = file;
                        SePolicyToolsGUI.log("工作目录切换到" + file.getPath());
                        updatedSourceDir();
                    }
                }
            });

            clearLogButton = new JButton("清除日志");
            clearLogButton.setBackground(themeColor);
            clearLogButton.setFont(buttonFont);
            clearLogButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            logStringBuilder.delete(0, logStringBuilder.length());
                            log("");
                        }
                    });
                }
            });

            autoScrollButton = new JButton("自动滚动:开");
            autoScrollButton.setBackground(themeColor);
            autoScrollButton.setFont(buttonFont);
            autoScrollButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (autoScrollFlag) {
                        autoScrollFlag = false;
                        autoScrollButton.setText("自动滚动:关");
                        log("");
                    } else {
                        autoScrollFlag = true;
                        autoScrollButton.setText("自动滚动:开");
                        log("");
                    }
                }
            });

            selectSourcePanel = new JPanel();
            selectSourcePanel.setLayout(new FlowLayout());
            selectSourcePanel.add(sourceDirLabel);
            selectSourcePanel.add(sourceDirString);
            selectSourcePanel.add(selectDirButton);
            selectSourcePanel.add(clearLogButton);
            selectSourcePanel.add(autoScrollButton);
            selectSourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            contentPane.add(selectSourcePanel, BorderLayout.NORTH);
        }

        /*
            中间
         */
        {
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setColumns(57);
            textArea.setRows(15);
            textArea.setFont(textFont);
            textArea.setEditable(false);

            // 滚动面板
            centerPanel = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 10));
//            centerPanel.setAutoscrolls(true);
            contentPane.add(centerPanel, BorderLayout.CENTER);
        }

        /*
            东边
         */
        {
            formatTEFilesButton = new JButton("格式化TE文件");
            formatTEFilesButton.setFont(buttonFont);
            formatTEFilesButton.setBackground(themeColor);
            formatTEFilesButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new formatTEFilesFunction());
                }
            });

            reWriteTEFilesButton = new JButton("清理并重命名TE文件");
            reWriteTEFilesButton.setBackground(themeColor);
            reWriteTEFilesButton.setFont(buttonFont);
            reWriteTEFilesButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new reWriteTEFilesFunction());
                }
            });

            formatFileContextsButton = new JButton("处理file_contexts");
            formatFileContextsButton.setFont(buttonFont);
            formatFileContextsButton.setBackground(themeColor);
            formatFileContextsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new formatFileContextsFunction());
                }
            });

            formatAllContextsFileButton = new JButton("格式化Context");
            formatAllContextsFileButton.setFont(buttonFont);
            formatAllContextsFileButton.setBackground(themeColor);
            formatAllContextsFileButton.addActionListener(new ActionListener() {
                public int i = 0;

                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new formatAllContextsFileFunction());
                }
            });

            logToSePolicyButton = new JButton("抓取log生成sePolicy");
            logToSePolicyButton.setFont(buttonFont);
            logToSePolicyButton.setBackground(themeColor);

            logToSePolicyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    threadPoolExecutor.execute(new logToSePolicyFunction());
                }
            });

            autoRun = new JButton("自动运行");
            autoRun.setBackground(themeColor);
            autoRun.setFont(buttonFont);
            autoRun.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 用新的线程去执行操作，否则界面会卡住
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {

                            // 格式化全部contexts
                            threadPoolExecutor.execute(new formatFileContextsFunction());
                            threadPoolExecutor.execute(new formatAllContextsFileFunction());

                            // 重写te文件
                            threadPoolExecutor.execute(new reWriteTEFilesFunction());

                            // 格式化te文件
                            threadPoolExecutor.execute(new formatTEFilesFunction());

                        }
                    });
                }
            });

            GridLayout gridLayout = new GridLayout(10, 1);
            gridLayout.setVgap(15);
            gridLayout.setHgap(15);
            eastPanel = new JPanel();
            eastPanel.setLayout(gridLayout);
            eastPanel.add(formatTEFilesButton);
            eastPanel.add(formatFileContextsButton);
            eastPanel.add(reWriteTEFilesButton);
            eastPanel.add(formatAllContextsFileButton);
            eastPanel.add(logToSePolicyButton);
            eastPanel.add(autoRun);
            eastPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 10));
            contentPane.add(eastPanel, BorderLayout.EAST);
        }

        setContentPane(contentPane);

        new DropTarget(textArea, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    try {
                        @SuppressWarnings("unchecked") java.util.List<File> list = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                        for (File file : list) {
                            if (file.isDirectory()) {
                                if (!file.getPath().equals(sourceFile.getPath())) log("切换工作目录");
                                sourceFile = file;
                                updatedSourceDir();
                                break;
                            }
                        }
                    } catch (UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                    }

                    dtde.dropComplete(true);
                } else {
                    dtde.rejectDrop();
                }
            }
        });

        setTitle("SePolicy工具");

        setResizable(false);

        setFont(globalFont);

        setBounds(200, 100, 1050, 600);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();

        setVisible(true);
    }

    /**
     * 界面日志输出
     */
    public static void log(String log) {
        if (logStringBuilder.length() != 0) {
            // 自动换行
            if (!log.isEmpty()) logStringBuilder.append("\r\n");
        }

        logStringBuilder.append(log);

        // 更新到界面
        if (textArea != null) {
            textArea.setText(logStringBuilder.toString());
            if (autoScrollFlag) textArea.setCaretPosition(logStringBuilder.length());
        }
    }

    public static void main(String[] args) {
        new SePolicyToolsGUI();
    }

    /**
     * 更新路径显示
     */
    private void updatedSourceDir() {
        sourceDirString.setText(sourceFile.getAbsolutePath());
    }

    /**
     * 抓取log生成sePolicy政策
     */
    private class logToSePolicyFunction implements Runnable {

        @Override
        public void run() {
            lock.lock();
            try {
                if (!hasRun) {
                    hasRun = true;
                    AdbUtilsImpl adbUtils = new AdbUtilsImpl();
                    CreateRuleFromLogImpl createRuleFromLog = new CreateRuleFromLogImpl();

                    // 抓取log
                    String[] logcat = adbUtils.logcat();

                    // 生成政策
                    String[] allowPolicy = createRuleFromLog.logToSePolicy(logcat);

                    if (allowPolicy != null) {
                        log("========================生成物===========================");
                        for (String line : allowPolicy) {
                            log(line);
                        }
                        log("=========================================================");

                        if (allowPolicy.length > 0) {
                            File file = new File("result.txt");
                            if (!file.exists()) {
                                new StreamHelperImpl().writeToFile(allowPolicy, file.getPath());
                                log("结果已另存为 " + file.getAbsolutePath());
                            } else {
                                log("发现已存在文件 " + file.getAbsolutePath() + " 故未另存结果");
                            }
                        }
                    }

                    log("完成");
                    hasRun = false;
                } else {
                    double random = Math.random() * info.length;
                    log(info[(int) random]);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 格式化file_contexts文件
     */
    private class formatFileContextsFunction implements Runnable {

        @Override
        public void run() {
            lock.lock();
            try {
                if (!hasRun) {
                    hasRun = true;
                    String fileContextsPath = new FilePathUtilsImpl().catPath(sourceFile.getPath(), "file_contexts");
                    File file = new File(fileContextsPath);
                    if (!file.exists()) {
                        SePolicyToolsGUI.log(fileContextsPath + "文件不存在");
                    } else {
                        fileContextsFormat.autoFormatFileContexts(file.getAbsolutePath(), file.getAbsolutePath());
                        SePolicyToolsGUI.log("生成新文件" + file.getPath());
                    }
                    SePolicyToolsGUI.log("完成");

                    hasRun = false;
                } else {
                    double random = Math.random() * info.length;
                    log(info[(int) random]);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 格式化所有Context文件
     */
    private class formatAllContextsFileFunction implements Runnable {

        @Override
        public void run() {
            lock.lock();
            try {
                if (!hasRun) {
                    hasRun = true;
                    File dir = new File(sourceFile.getPath());
                    if (!dir.exists()) {
                        SePolicyToolsGUI.log(sourceFile.getPath() + "文件不存在");
                        SePolicyToolsGUI.log("未执行任何操作");
                    } else {
                        contextsUtils.autoFormatAllContext(dir.getAbsolutePath());
                        SePolicyToolsGUI.log("完成");
                    }
                    hasRun = false;
                } else {
                    double random = Math.random() * info.length;
                    log(info[(int) random]);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 重写te文件
     */
    private class reWriteTEFilesFunction implements Runnable {

        @Override
        public void run() {
            lock.lock();
            try {
                if (!hasRun) {
                    hasRun = true;
                    if (!new File(sourceFile.getPath()).exists()) {
                        SePolicyToolsGUI.log(sourceFile.getPath() + "不存在");
                    } else {
                        sePolicyDirUtils.reWriteTeFiles(sourceFile.getPath(), sourceFile.getPath(), new FilePathUtilsImpl().catPath(sourceFile.getPath(), "file_contexts"));
                        SePolicyToolsGUI.log("完成");
                    }
                    hasRun = false;
                } else {
                    double random = Math.random() * info.length;
                    log(info[(int) random]);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 格式化te文件
     */
    private class formatTEFilesFunction implements Runnable {

        @Override
        public void run() {
            lock.lock();
            try {
                if (!hasRun) {
                    hasRun = true;

                    if (!new File(sourceFile.getPath()).exists()) {
                        SePolicyToolsGUI.log(sourceFile.getPath() + "不存在");
                    } else {
                        sePolicyDirUtils.formatFiles(sourceFile.getAbsolutePath(), sourceFile.getAbsolutePath());
                        SePolicyToolsGUI.log("完成");
                    }
                    hasRun = false;
                } else {
                    double random = Math.random() * info.length;
                    log(info[(int) random]);
                }
            } finally {
                lock.unlock();
            }
        }

    }

    /**
     * 文件选择弹窗
     */
    private File selectDirDialog() {
        JFileChooser jFileChooser = new JFileChooser(".");
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = jFileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return jFileChooser.getSelectedFile();
        }
        return null;
    }
}
