package com.example.notebookllm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectScanner {
    // 添加日志实例
    private static final Logger logger = LoggerFactory.getLogger(ProjectScanner.class);
    
    // 需要忽略的文件夹名称
    private static final String[] IGNORED_DIRECTORIES = {
        "target",           // Maven构建输出目录
        "node_modules",     // Node.js依赖目录
        ".git",             // Git版本控制目录
        ".svn",             // SVN版本控制目录
        ".gradle",          // Gradle缓存目录
        "build",            // Gradle构建输出目录
        "dist",             // 常见的构建输出目录
        "out",              // IntelliJ IDEA输出目录
        "bin",              // 二进制文件目录
        ".idea",            // IntelliJ IDEA配置目录
        ".vscode"           // VS Code配置目录
    };
    
    public static List<File> scanProjects(File root) {
        logger.info("开始扫描项目目录: {}", root.getAbsolutePath());
        List<File> projects = new ArrayList<>();
        scanRecursive(root, projects);
        logger.info("项目扫描完成，共找到 {} 个项目", projects.size());
        
        // 记录找到的项目路径
        for (File project : projects) {
            logger.debug("找到项目: {}", project.getAbsolutePath());
        }
        
        return projects;
    }

    private static void scanRecursive(File dir, List<File> projects) {
        if (dir.isDirectory()) {
            // 检查是否是需要忽略的目录
            String dirName = dir.getName();
            
            // 忽略固定名称的目录
            for (String ignoredDir : IGNORED_DIRECTORIES) {
                if (ignoredDir.equalsIgnoreCase(dirName)) {
                    logger.debug("跳过忽略的目录: {}", dir.getAbsolutePath());
                    return;
                }
            }
            
            // 忽略以.plugin开头的目录
            if (dirName.toLowerCase().startsWith(".plugin")) {
                logger.debug("跳过.plugin开头的目录: {}", dir.getAbsolutePath());
                return;
            }
            
            File[] files = dir.listFiles();
            if (files == null) {
                logger.warn("无法列出目录内容: {}", dir.getAbsolutePath());
                return;
            }
            
            // 检查是否为空目录
            if (files.length == 0) {
                logger.debug("跳过空目录: {}", dir.getAbsolutePath());
                return;
            }
            
            boolean isProject = false;
            for (File f : files) {
                if (f.getName().equalsIgnoreCase("pom.xml") ||
                    f.getName().equalsIgnoreCase("requirements.txt") ||
                    f.getName().equalsIgnoreCase("README.md") ||
                    f.getName().equalsIgnoreCase("build.gradle") ||
                    f.getName().equalsIgnoreCase("package.json")) {
                    isProject = true;
                    logger.debug("识别到项目目录: {}", dir.getAbsolutePath());
                    break;
                }
            }
            
            if (isProject) {
                projects.add(dir);
                logger.info("添加项目目录: {}", dir.getAbsolutePath());
            }
            
            // 递归扫描子目录
            for (File f : files) {
                if (f.isDirectory()) {
                    scanRecursive(f, projects);
                }
            }
        }
    }
}