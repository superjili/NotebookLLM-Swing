package com.example.notebookllm;

import java.io.*;
import java.util.Properties;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    // 添加日志实例
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String CONFIG_FILE = "notebookllm.properties";
    private final Properties props = new Properties();

    public ConfigManager() {
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
                logger.info("配置文件加载成功: {}", CONFIG_FILE);
            } catch (IOException e) {
                logger.error("配置文件加载失败: {}", CONFIG_FILE, e);
            }
        } else {
            logger.info("配置文件不存在，使用默认配置: {}", CONFIG_FILE);
        }
    }

    public String get(String key, String def) {
        String value = props.getProperty(key, def);
        logger.debug("获取配置项 - Key: {}, Value: {}", key, value);
        return value;
    }

    public void set(String key, String value) {
        props.setProperty(key, value == null ? "" : value);
        logger.debug("设置配置项 - Key: {}, Value: {}", key, value);
    }

    public void save() throws IOException {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "NotebookLLM config");
            logger.info("配置文件保存成功: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("配置文件保存失败: {}", CONFIG_FILE, e);
            throw e;
        }
    }
    
    // 添加获取模型名称的方法
    public String getModel() {
        String model = get("api.model", "qwen3-32b-fp8");
        logger.debug("获取模型配置: {}", model);
        return model;
    }
    
    // 添加设置模型名称的方法
    public void setModel(String model) {
        set("api.model", model);
        logger.debug("设置模型配置: {}", model);
    }
    
    // 添加获取默认API URL的方法
    public String getDefaultApiUrl() {
        return "http://192.168.11.151:8091/v1/chat/completions";
    }
}