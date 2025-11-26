package com.example.notebookllm;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}