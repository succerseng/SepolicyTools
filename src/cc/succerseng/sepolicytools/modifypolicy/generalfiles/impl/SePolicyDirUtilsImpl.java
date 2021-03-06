package cc.succerseng.sepolicytools.modifypolicy.generalfiles.impl;

import cc.succerseng.sepolicytools.modifypolicy.contextfiles.ContextsUtils;
import cc.succerseng.sepolicytools.modifypolicy.contextfiles.impl.ContextsUtilsImpl;
import cc.succerseng.sepolicytools.modifypolicy.generalfiles.SePolicyDirUtils;
import cc.succerseng.sepolicytools.modifypolicy.generalfiles.SePolicyFileUtils;
import cc.succerseng.sepolicytools.utils.FilePathUtils;
import cc.succerseng.sepolicytools.utils.Logger;
import cc.succerseng.sepolicytools.utils.impl.FilePathUtilsImpl;
import cc.succerseng.sepolicytools.utils.impl.LoggerImpl;
import cc.succerseng.sepolicytools.utils.impl.StreamHelperImpl;

import java.io.File;
import java.util.*;

public class SePolicyDirUtilsImpl implements SePolicyDirUtils {
    // tools
    private final SePolicyFileUtils sePolicyFileUtils = new SePolicyFileUtilsImpl();
    private final FilePathUtils filePathUtils = new FilePathUtilsImpl();
    private final ContextsUtils contextsUtils = new ContextsUtilsImpl();
    private final StreamHelperImpl streamHelper = new StreamHelperImpl();
    private final Logger logger = new LoggerImpl();

    public static void main(String[] args) {
        new SePolicyDirUtilsImpl().reWriteTeFiles("sepolicy", "sepolicy", "sepolicy/file_contexts");
    }

    /**
     * 从多个文件中读取
     *
     * @param files 文件列表
     * @return 合并的文件内容
     */
    private String[] readAllLineFromFiles(String inputDir, String[] files) {
        ArrayList<String> arrayList = new ArrayList<>();
        String[] result = null;

        for (String file : files) {
            String[] strings = sePolicyFileUtils.readAllLineFromTEFile(new FilePathUtilsImpl().catPath(inputDir, file));
            arrayList.addAll(Arrays.asList(strings));
        }

        result = new String[arrayList.size()];
        arrayList.toArray(result);

        return result;
    }

    /**
     * 得到dir下TE文件的相对路径
     *
     * @return 列表
     */
    private String[] getTEFileList(String dir) {
        File directory = new File(dir);
        List<String> list = new ArrayList<>();
        String[] result = null;

        if (!directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        String path = null;
        for (File f : files) {
            path = f.getAbsolutePath();
            if (path.endsWith(".te")) {
                list.add(f.getName());
            }
        }

        result = new String[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * 格式化整个文件夹下的te文件
     * 列出文件列表，再格式化
     *
     * @param inPutDir  存有te文件的文件夹路径
     * @param outPutDir 输出文件夹路径
     */
    @Override
    public void formatFiles(String inPutDir, String outPutDir) {
        String[] tEFileList = getTEFileList(inPutDir);

        if (tEFileList == null) {
            logger.println("获取" + inPutDir + "失败");
            return;
        }

        logger.println("在" + inPutDir + "共找到" + tEFileList.length + "个te文件");
        if (tEFileList.length == 0) return;

        File outPutFd = new File(outPutDir);

        if (!outPutFd.exists()) {
            logger.println("创建文件夹" + outPutFd);
            boolean mkdirs = outPutFd.mkdirs();
            if (!mkdirs) {
                logger.println("文件夹" + outPutFd + "创建失败");
            }
        }

        for (String file : tEFileList) {
            logger.println("格式化" + file);
            sePolicyFileUtils.autoFormatFile(filePathUtils.catPath(inPutDir, file), filePathUtils.catPath(outPutDir, file));
            logger.println("格式化" + file + "完成");
        }
    }

    /**
     * 将重新创建te文件
     *
     * @param inPutDir      sepolicy文件夹
     * @param outPutDir     输出文件夹
     * @param file_contexts 描述文件
     */
    @Override
    public void reWriteTeFiles(String inPutDir, String outPutDir, String file_contexts) {

        // 验证选择的是SePolicy文件夹
        File[] inDir = new File(inPutDir).listFiles();

        int teCount = 0;
        for (; inDir != null && teCount < inDir.length; teCount++) {
            if (inDir[teCount].getName().endsWith("te")) break;
        }

        if (inDir == null || teCount == inDir.length) {
            logger.println("这 " + inPutDir + " TM是个什么玩意");
            return;
        }

        // 存储一个标签对应的所有语句
        Map<String, TreeSet<String>> labelMap = new HashMap<>();

        // 存储语句
        TreeSet<String> labelLines = null;

        // 得到te文件列表
        String[] teFileList = getTEFileList(inPutDir);
        if (teFileList == null) {
            logger.println("没有找到任何te文件" + inPutDir);
            return;
        }

        // 得到标签
        String[] labelFromContexts = contextsUtils.getAllLabels(inPutDir);

        // 得到te文件内容
        String[] allContents = readAllLineFromFiles(inPutDir, teFileList);

        // 输入与输出的是同一个文件夹就删除旧te文件
        if (inPutDir.equals(outPutDir)) {
            for (String file : teFileList) {
                boolean delete = new File(file).delete();
                if (!delete) {
                    logger.println("删除" + file + "失败");
                }
            }
        }

        // 关联标签与包含此标签的行
        for (String label : labelFromContexts) {
            labelLines = new TreeSet<>(new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    if (s.equals(t1)) return 0;

                    if (s.startsWith("type ")) {
                        return -1;
                    }

                    if (t1.startsWith("type ")) {
                        return 1;
                    }

                    return t1.compareTo(s);
                }
            });

            for (String line : allContents) {
                if (line.contains(label)) {
                    labelLines.add(line);
                }
            }

            // 将 aaa_bbbb_cccccc 中的aaa作为标签，如果没有_就整个作为标签
            if (label.contains("_")) label = label.substring(0, label.indexOf("_"));

            // 如果key已存在则要添加上已添加的数据，再放回map
            if (labelMap.containsKey(label)) {
                TreeSet<String> oldData = labelMap.get(label);
                labelLines.addAll(oldData);
            }

            labelMap.put(label, labelLines);
        }

        // 写入新文件
        for (String lable : labelMap.keySet()) {
            String path = filePathUtils.catPath(new File(outPutDir).getPath(), lable) + ".te";

            TreeSet<String> treeSet = labelMap.get(lable);
            String[] content = new String[treeSet.size()];
            treeSet.toArray(content);

            streamHelper.writeToFile(content, path);

            // 删除空文件
            File newFile = new File(path);
            if (newFile.length() == 0) {
                boolean delete = newFile.delete();
                if (!delete) {
                    logger.println("删除" + newFile.getName() + "失败");
                }
            }
        }
    }

}
