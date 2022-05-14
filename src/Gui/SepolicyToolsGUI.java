package Gui;

import ContextFileTools.Impl.FileContextsFormatImpl;
import GeneralFilesTools.Impl.SepolicyDirUtilsImpl;
import GeneralFilesTools.SepolicyDirUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * 图形界面
 */
public class SepolicyToolsGUI extends JFrame {
    private final SepolicyDirUtils sepolicyDirUtils = new SepolicyDirUtilsImpl();
    private final FileContextsFormatImpl fileContextsFormat = new FileContextsFormatImpl();

    private final Font globalFont = new Font(null, Font.BOLD, 30);
    private final Font textFont = new Font(null, Font.BOLD, 20);
    private final Font buttonFont = new Font(null, Font.BOLD, 15);
    private final Color themeColor = new Color(195, 233, 242);

    private File sourceFile = new File(".");

    private static StringBuilder logStringBuilder = new StringBuilder();

    private Container contentPane = null;
    private JLabel sourceDirLabel = null;
    private JTextField sourceDirString = null;
    private JButton selectDirButton = null;
    private JPanel selectSourcePanel = null;
    private static JTextArea textArea = null;
    private JButton formatTEFilesButton = null;
    private JButton formatFileContextsButton = null;
    private JPanel leftPanel = null;
    private JPanel rightPanel = null;

    /**
     * 界面日志输出
     */
    public static void log(String log) {
        while (log.length() > 50) {
            logStringBuilder.append(log.substring(0, 50));
            logStringBuilder.append("\r\n");
            log = log.substring(50, log.length());
        }

        logStringBuilder.append(log);
        logStringBuilder.append("\r\n");

        textArea.setText(logStringBuilder.toString());
        if (logStringBuilder.length() > 250) logStringBuilder.delete(0, logStringBuilder.length());
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

    /**
     * 界面
     */
    public SepolicyToolsGUI() {
        contentPane = getContentPane();

        sourceDirLabel = new JLabel("工作路径:");
        sourceDirLabel.setFont(textFont);

        sourceDirString = new JTextField(35);
        sourceDirString.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File path = new File(sourceDirString.getText());

                if (path.exists()) {
                    sourceFile = path;
                    SepolicyToolsGUI.log("切换文件路径" + sourceFile.getAbsolutePath());
                }

                sourceDirString.setText(sourceFile.getAbsolutePath());
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
                    SepolicyToolsGUI.log("未选择文件");
                } else {
                    sourceFile = file;
                    SepolicyToolsGUI.log("工作目录切换到" + file.getPath());
                    sourceDirString.setText(sourceFile.getAbsolutePath());
                }
            }
        });


        selectSourcePanel = new JPanel();
        selectSourcePanel.add(sourceDirLabel);
        selectSourcePanel.add(sourceDirString);
        selectSourcePanel.add(selectDirButton);

        contentPane.add(selectSourcePanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setColumns(35);
        textArea.setRows(20);
        textArea.setFont(textFont);
        textArea.setEditable(false);

        formatTEFilesButton = new JButton("格式化TE文件");
        formatTEFilesButton.setFont(buttonFont);
        formatTEFilesButton.setBackground(themeColor);
        formatTEFilesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!new File(sourceFile.getPath()).exists()) {
                    SepolicyToolsGUI.log(sourceFile.getPath() + "不存在");
                }
                SepolicyToolsGUI.log("开始");
                sepolicyDirUtils.formatFiles(sourceFile.getAbsolutePath(), sourceFile.getAbsolutePath());
                SepolicyToolsGUI.log("完成！！！");
            }
        });

        formatFileContextsButton = new JButton("处理file_contexts文件");
        formatFileContextsButton.setFont(buttonFont);
        formatFileContextsButton.setBackground(themeColor);
        formatFileContextsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileContextsPath = sourceFile.getPath() + "/file_contexts";
                File file = new File(fileContextsPath);
                if (!file.exists()) {
                    SepolicyToolsGUI.log(fileContextsPath + "文件不存在");
                    SepolicyToolsGUI.log("未执行任何操作");
                } else {
                    SepolicyToolsGUI.log("处理文件" + file.getPath());
                    fileContextsFormat.autoFormatFileContexts(file.getAbsolutePath(), file.getAbsolutePath());
                    SepolicyToolsGUI.log("新文件" + file.getPath());
                    SepolicyToolsGUI.log("完成");
                }
            }
        });

        leftPanel = new JPanel();
        leftPanel.add(textArea);
        contentPane.add(leftPanel, BorderLayout.WEST);

        rightPanel = new JPanel();
        rightPanel.add(formatTEFilesButton);
        rightPanel.add(formatFileContextsButton);
        contentPane.add(rightPanel, BorderLayout.EAST);


        setTitle("Sepolicy工具");
        setResizable(false);
        setFont(globalFont);
        setBounds(200, 100, 1000, 618);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new SepolicyToolsGUI();
    }
}
