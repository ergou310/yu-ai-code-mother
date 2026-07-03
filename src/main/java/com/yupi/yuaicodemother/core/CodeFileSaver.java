package com.yupi.yuaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件保存器
 */
@Deprecated
public class CodeFileSaver {

    // 文件保存的根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HTML 网页代码
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String baseDir = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDir, "index.html", htmlCodeResult.getHtmlCode());
        return new File(baseDir);
    }

    // 保存多文件代码
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDir = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDir, "index.html", result.getHtmlCode());
        writeToFile(baseDir, "style.css", result.getCssCode());
        writeToFile(baseDir, "script.js", result.getJsCode());
        return new File(baseDir);
    }

    /**
     * 构建文件的唯一路径： tmp/code_output/bizType_雪花 ID
     * @param bizType 代码生成类型
     * @return
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}",bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件
     * @param dirPath
     * @param fileName
     * @param content
     */
    private static void writeToFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
