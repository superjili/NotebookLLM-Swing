package com.example.notebookllm;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class HistoryManager {
    // 添加日志实例
    private static final Logger logger = LoggerFactory.getLogger(HistoryManager.class);
    
    private static final String JDBC_URL = "jdbc:h2:./notebookllm_history";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public HistoryManager() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            // 直接创建新表结构（如果不存在）
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS analysis_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "project_path VARCHAR(255), " +
                    "project_name VARCHAR(255), " +
                    "project_description VARCHAR(1000), " +
                    "result CLOB, " +
                    "analyzed_at TIMESTAMP)");
            logger.info("数据库初始化完成，历史记录表已准备就绪");
        } catch (SQLException e) {
            logger.error("数据库初始化失败", e);
            throw new RuntimeException(e);
        }
    }

    public void save(AnalysisResult result) {
        logger.debug("保存分析结果到数据库 - 项目路径: {}", result.projectPath);
        
        // 确保项目名称和描述是从分析结果中提取的
        result.extractProjectInfoFromResult();
        
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            // 使用新的表结构保存数据
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO analysis_history (project_path, project_name, project_description, result, analyzed_at) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, result.projectPath);
            ps.setString(2, result.projectName);
            ps.setString(3, result.projectDescription);
            ps.setString(4, result.result);
            ps.setTimestamp(5, Timestamp.valueOf(result.analyzedAt));
            ps.executeUpdate();
            logger.info("分析结果保存成功 - 项目路径: {}", result.projectPath);
        } catch (SQLException e) {
            logger.error("分析结果保存失败 - 项目路径: {}", result.projectPath, e);
            throw new RuntimeException(e);
        }
    }

    public List<AnalysisResult> list() {
        List<AnalysisResult> results = new ArrayList<>();
        logger.debug("从数据库查询历史记录");
        
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            // 查询所有字段
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, project_path, project_name, project_description, result, analyzed_at FROM analysis_history ORDER BY analyzed_at DESC");
            while (rs.next()) {
                // 使用新的构造函数，直接传入从数据库读取的项目名称和描述
                results.add(new AnalysisResult(
                    rs.getInt("id"),
                    rs.getString("project_path"),
                    rs.getString("project_name"),
                    rs.getString("project_description"),
                    rs.getString("result"),
                    rs.getTimestamp("analyzed_at").toLocalDateTime()
                ));
            }
            logger.info("历史记录查询成功，共找到 {} 条记录", results.size());
        } catch (SQLException e) {
            logger.error("历史记录查询失败", e);
            throw new RuntimeException(e);
        }
        return results;
    }
    
    /**
     * 导出历史记录为CSV格式
     * @param filePath 导出文件路径
     * @throws IOException 如果文件写入失败
     */
    public void exportToCSV(String filePath) throws IOException {
        logger.info("开始导出历史记录为CSV格式: {}", filePath);
        
        List<AnalysisResult> results = list();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // 写入CSV头部
            writer.write("ID,项目路径,项目名称,项目描述,分析时间,结果摘要\n");
            
            // 写入数据行
            for (AnalysisResult result : results) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    result.id,
                    escapeCsv(result.projectPath),
                    escapeCsv(result.projectName),
                    escapeCsv(result.projectDescription),
                    result.analyzedAt.format(formatter),
                    escapeCsv(getSummary(result.result, 200))
                ));
            }
            
            logger.info("CSV导出成功，共导出 {} 条记录", results.size());
        }
    }
    
    /**
     * 导出历史记录为JSON格式
     * @param filePath 导出文件路径
     * @throws IOException 如果文件写入失败
     */
    public void exportToJSON(String filePath) throws IOException {
        logger.info("开始导出历史记录为JSON格式: {}", filePath);
        
        List<AnalysisResult> results = list();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        ArrayNode arrayNode = mapper.createArrayNode();
        
        for (AnalysisResult result : results) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", result.id);
            node.put("projectPath", result.projectPath);
            node.put("projectName", result.projectName);
            node.put("projectDescription", result.projectDescription);
            node.put("analyzedAt", result.analyzedAt.format(formatter));
            
            // 尝试将result字段解析为JSON，如果失败则作为字符串
            try {
                Object resultObj = mapper.readValue(result.result, Object.class);
                node.set("result", mapper.valueToTree(resultObj));
            } catch (Exception e) {
                node.put("result", result.result);
            }
            
            arrayNode.add(node);
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            mapper.writeValue(writer, arrayNode);
            logger.info("JSON导出成功，共导出 {} 条记录", results.size());
        }
    }
    
    /**
     * 转义CSV特殊字符
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 替换双引号为两个双引号，并移除换行符
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }
    
    /**
     * 获取文本摘要
     */
    private String getSummary(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}