package cc.succerseng.sepolicytools.utils.impl;


import cc.succerseng.sepolicytools.utils.AboutSystem;
import cc.succerseng.sepolicytools.utils.AdbUtils;
import cc.succerseng.sepolicytools.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdbUtilsImpl implements AdbUtils {
    private static String allFileList = null;
    private static final AboutSystem aboutSystem = new AboutSystemImpl();
    private static final Logger logger = new LoggerImpl();

    /**
     * 根据系统类型 确定adb工具的路径~
     */
    private static String getADBTool() {
        int systemType = aboutSystem.getSystemType();

        String adbPath = null;
        switch (systemType) {
            case AboutSystem.WINDOWS:
                adbPath = "platform-tools/windows/adb.exe";
                break;
            case AboutSystem.LINUX:
                adbPath = "platform-tools/linux/adb";
                if (!new File(adbPath).canExecute()) {
                    logger.println(adbPath + "couldn't execute");
                    adbPath = null;
                }
                break;
        }
        return adbPath;
    }

    /**
     * 测试
     * 需要连接手机
     */
    public static void main(String[] args) {
        System.out.println(new AdbUtilsImpl().flashImage("image", "/tmp/image"));
    }

    /**
     * 获得当前已经连接的设备数量
     *
     * @return 设备数量
     */
    int getConnectedDeviceNum() {
        int count = 0;

        String adb = getADBTool();
        if (adb == null) {
            return 0;
        }

        Process exec = null;
        BufferedReader bufferedReader = null;
        try {
            exec = Runtime.getRuntime().exec(getADBTool() + " devices");
            bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            // 丢掉错误输出
            while (exec.getErrorStream().available() > 0) {
                exec.getErrorStream().read();
            }

            // 统计数量
            String buffer;
            while ((buffer = bufferedReader.readLine()) != null) {
                if (!buffer.isEmpty()) count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return count - 1;
    }

    /**
     * 执行shell命令
     */
    public String[] shell(String command) {
        return execute("shell " + command);
    }

    /**
     * 通过adb获取全部文件列表
     *
     * @return 结果
     */
    public String getAllFileList() {
        if (allFileList == null) {
            logger.println("正在获取文件列表..");

            String[] shell = shell("ls -R");
            if (shell == null) {
                return null;
            }

            logger.println("正在处理结果");
            StringBuilder result = new StringBuilder();

            // 把“ls -R”得到的结果数组转成一个字符串 并给每一个文件加上完整路径
            int i = 0;
            while (i < shell.length - 1) {
                if (shell[i].isEmpty() && !shell[i + 1].isEmpty()) {
                    i++;
                    String line = shell[i]; // line 为文件夹路径
                    i++;
                    while (i < shell.length && !shell[i].isEmpty()) {
                        if (line.endsWith(":")) line = line.substring(0, line.lastIndexOf(":")); // 砍掉文件夹后面的冒号

                        result.append(line).append("/").append(shell[i]);// 拼接文件完整路径
                        result.append("\r\n");
                        i++;
                    }
                } else {
                    i++;
                }
            }

            allFileList = result.toString();
        }

        return allFileList;
    }

    /**
     * 判断文件是否存在
     */
    public boolean isExisted(String path) {
        String allFileList = getAllFileList();

        // 得不到列表默认存在
        if (allFileList == null) return true;

        Pattern pattern = Pattern.compile(path);
        Matcher matcher = pattern.matcher(allFileList);
        return matcher.find();
    }

    /**
     * 执行adb命令
     *
     * @param command 命令
     * @return 结果
     */
    @Override
    public String[] execute(String command) {
        return execute(command, Long.MAX_VALUE);
    }

    /**
     * 抓取一万条log
     *
     * @return log
     */
    @Override
    public String[] logcat() {
        logger.println("正在抓取log");
        String[] logcats = execute("logcat", 10000);
        if (logcats == null) {
            logger.println("没有抓到log哟~");
        }
        return logcats;
    }

    /**
     * 执行adb命令
     *
     * @param command 命令
     * @param line    限制抓取的行数
     * @return 结果
     */
    @Override
    public String[] execute(String command, long line) {

        // 检查设备数量
        int num = getConnectedDeviceNum();
        if (num == 0) {
            logger.println("当前设备数量 " + num + " 请连接设备");
            return null;
        }

        if (num > 1) {
            logger.println("当前设备数量 " + num + " 请减少设备");
            return null;
        }

        Process exec = null;
        BufferedReader bufferedReader = null;
        try {
            exec = Runtime.getRuntime().exec(getADBTool() + " " + command);
            bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            ArrayList<String> arrayList = new ArrayList<>();
            String buffer = null;
            while ((buffer = bufferedReader.readLine()) != null && line-- > 0) {
                //logger.log(Level.INFO, buffer);
                arrayList.add(buffer);

                // 丢掉错误输出，不然上面卡死！
                while (exec.getErrorStream().available() > 0) exec.getErrorStream().read();
            }

            String[] strings = new String[arrayList.size()];
            arrayList.toArray(strings);

            return strings;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public boolean flashImage(String image, String blk_dev) {
        String imagePath = null; // 推送img到设备的位置
        String blk_dev_location = blk_dev;

        if (!new File(image).exists()) {
            logger.println("镜像" + image + "不存在");
            return false;
        }

        if (aboutSystem.getSystemType() == AboutSystem.WINDOWS) {
            imagePath = "//tmp/image";
            if (blk_dev.startsWith("/")) {
                blk_dev_location = "/" + blk_dev;
            }
        } else {
            imagePath = "/tmp/image";
        }

        execute("push " + image + " " + imagePath);

        if (isExisted("/tmp/image")) {
            if (!isExisted(blk_dev)) {
                logger.println("设备" + blk_dev + "不存在");
                return false;
            }

            String[] ddResult = execute("shell dd" + " if=" + imagePath + " of=" + blk_dev_location);
            if (ddResult.length == 0) {
                logger.println(imagePath + "刷入" + blk_dev + "成功");
                return true;
            }
        } else {
            logger.println("推送" + image + "失败");
        }

        return false;
    }

}
