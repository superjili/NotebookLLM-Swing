package com.example.notebookllm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.io.File;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    // 添加日志实例
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    // 定义主题颜色
    private static final Color PRIMARY_COLOR = new Color(0, 150, 255); // QQ蓝
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color PANEL_COLOR = Color.WHITE;
    private static final Color BUTTON_COLOR = new Color(0, 150, 255);
    private static final Color BUTTON_HOVER_COLOR = new Color(0, 130, 230);
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE; // 按钮文字颜色
    private static final Color TEXT_COLOR = new Color(51, 51, 51); // 主要文字颜色
    private static final Color LABEL_TEXT_COLOR = new Color(70, 70, 70); // 标签文字颜色
    private static final Color BORDER_COLOR = new Color(220, 220, 220);
    
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("无法设置系统外观，使用默认外观", e);
        }
        
        SwingUtilities.invokeLater(() -> {
            logger.info("启动NotebookLLM应用程序");
            
            JFrame frame = new JFrame("NotebookLLM");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700);
            frame.setLayout(new BorderLayout());
            
            // 设置窗口居中
            frame.setLocationRelativeTo(null);
            
            // 设置窗口装饰（仿QQ风格）
            try {
                // 只有在支持窗口透明度的情况下才设置窗口形状
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                
                if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
                    // 取消窗口装饰以支持自定义形状
                    frame.setUndecorated(true);
                    
                    // 设置窗口透明度
                    frame.setOpacity(1.0f);
                    
                    // 设置窗口形状（圆角）
                    frame.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
                }
            } catch (Exception e) {
                logger.warn("无法设置窗口形状", e);
            }

            // 创建主面板
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(BACKGROUND_COLOR);
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JButton selectBtn = createStyledButton("选择文件夹");
            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> projectList = new JList<>(listModel);
            projectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            styleListComponent(projectList);

            JTextArea output = new JTextArea();
            output.setEditable(false);
            output.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            output.setLineWrap(true);
            output.setWrapStyleWord(true);
            output.setForeground(TEXT_COLOR); // 设置文字颜色
            output.setBackground(PANEL_COLOR); // 设置背景颜色
            JScrollPane scroll = new JScrollPane(output);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            styleScrollPane(scroll);

            JButton analyzeBtn = createStyledButton("分析所选");
            JButton analyzeAllBtn = createStyledButton("分析全部");
            JButton historyBtn = createStyledButton("查看历史");
            JButton exitBtn = createStyledButton("退出");

            selectBtn.addActionListener(e -> {
                logger.debug("用户点击选择文件夹按钮");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int ret = chooser.showOpenDialog(frame);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File dir = chooser.getSelectedFile();
                    logger.info("用户选择了目录: {}", dir.getAbsolutePath());
                    output.setText("扫描中: " + dir.getAbsolutePath() + "\n");
                    listModel.clear();
                    java.util.List<File> projects = ProjectScanner.scanProjects(dir);
                    for (File p : projects) listModel.addElement(p.getAbsolutePath());
                    output.append("扫描完成，发现 " + projects.size() + " 个项目\n");
                    logger.info("扫描完成，共发现 {} 个项目", projects.size());
                }
            });

            analyzeBtn.addActionListener(e -> {
                logger.debug("用户点击分析所选按钮");
                java.util.List<String> sel = projectList.getSelectedValuesList();
                if (sel.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "请先选择一个或多个项目。");
                    logger.warn("用户尝试分析项目但未选择任何项目");
                    return;
                }
                logger.info("开始分析 {} 个项目", sel.size());
                analyzeProjectsAsync(sel, output);
            });

            analyzeAllBtn.addActionListener(e -> {
                logger.debug("用户点击分析全部按钮");
                java.util.List<String> all = new java.util.ArrayList<>();
                for (int i = 0; i < listModel.size(); i++) all.add(listModel.getElementAt(i));
                if (all.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "项目列表为空，请先选择文件夹扫描。");
                    logger.warn("用户尝试分析全部项目但项目列表为空");
                    return;
                }
                logger.info("开始分析全部 {} 个项目", all.size());
                analyzeProjectsAsync(all, output);
            });

            historyBtn.addActionListener(e -> {
                logger.debug("用户点击查看历史按钮");
                showHistoryDialog(frame);
            });

            JButton settingsBtn = createStyledButton("设置 API");
            settingsBtn.addActionListener(e -> {
                logger.debug("用户点击设置API按钮");
                showSettingsDialog(frame);
            });

            // 添加退出按钮事件监听器
            exitBtn.addActionListener(e -> {
                logger.info("用户点击退出按钮，正在关闭应用程序");
                int option = JOptionPane.showConfirmDialog(frame, 
                    "确定要退出 NotebookLLM 吗？", 
                    "确认退出", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                
                if (option == JOptionPane.YES_OPTION) {
                    logger.info("用户确认退出应用程序");
                    System.exit(0);
                } else {
                    logger.debug("用户取消退出操作");
                }
            });

            // 创建顶部按钮面板
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            topPanel.setBackground(BACKGROUND_COLOR);
            topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            topPanel.add(selectBtn);
            topPanel.add(analyzeBtn);
            topPanel.add(analyzeAllBtn);
            topPanel.add(historyBtn);
            topPanel.add(settingsBtn);
            topPanel.add(exitBtn);

            // 创建左右分割面板
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            // 确保左右组件正确设置颜色
            JScrollPane leftScrollPane = new JScrollPane(projectList);
            leftScrollPane.getViewport().setBackground(PANEL_COLOR);
            split.setLeftComponent(leftScrollPane);
            split.setRightComponent(scroll);
            split.setDividerLocation(400);
            split.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            styleSplitPane(split);

            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(split, BorderLayout.CENTER);
            
            frame.add(mainPanel, BorderLayout.CENTER);
            frame.setVisible(true);
            
            logger.info("NotebookLLM应用程序主界面已显示");
        });
    }
    
    /**
     * 创建样式化按钮
     */
    private static JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            // 重写paintComponent方法以确保背景色正确显示
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(BUTTON_TEXT_COLOR); // 设置按钮文字颜色为白色
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true); // 确保背景色可见
        button.setBorderPainted(false); // 不绘制边框
        
        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER_COLOR);
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });
        
        return button;
    }
    
    /**
     * 样式化列表组件
     */
    private static void styleListComponent(JList<?> list) {
        list.setBackground(PANEL_COLOR);
        list.setForeground(TEXT_COLOR); // 设置列表文字颜色
        list.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        list.setSelectionBackground(PRIMARY_COLOR);
        list.setSelectionForeground(Color.WHITE);
    }
    
    /**
     * 样式化滚动面板
     */
    private static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        // 确保视口内容的背景色正确
        if (scrollPane.getViewport().getView() != null) {
            scrollPane.getViewport().getView().setBackground(PANEL_COLOR);
        }
    }
    
    /**
     * 样式化分割面板
     */
    private static void styleSplitPane(JSplitPane splitPane) {
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private static void analyzeProjectsAsync(java.util.List<String> projects, JTextArea output) {
        logger.debug("开始异步分析项目任务");
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ConfigManager cfg = new ConfigManager();
                String apiUrl = cfg.get("api.url", System.getenv().getOrDefault("OPENAI_API_URL", cfg.getDefaultApiUrl()));
                String apiKey = cfg.get("api.key", System.getenv().getOrDefault("OPENAI_API_KEY", "sk-xxx"));
                String model = cfg.getModel();
                
                logger.info("使用API配置 - URL: {}, Model: {}", apiUrl, model);
                
                if (apiUrl.isEmpty() || apiKey.isEmpty()) {
                    String msg = "未设置 API 地址或 Key，跳过调用大模型（请在设置中填写）。\n";
                    publish(msg);
                    logger.warn(msg);
                    return null;
                }
                
                LLMClient client = new LLMClient(apiUrl, apiKey, model);
                HistoryManager hm = new HistoryManager();
                
                for (String p : projects) {
                    String msg = "分析：" + p + "\n";
                    publish(msg);
                    logger.info("开始分析项目: {}", p);
                    
                    try {
                        String prompt = buildPromptForProject(new File(p));
                        logger.debug("为项目 {} 构建提示完成，提示长度: {}", p, prompt.length());
                        
                        StringBuilder streamed = new StringBuilder();
                        // Stream display
                        try {
                            client.analyzeStream(prompt, chunk -> {
                                streamed.append(chunk);
                                publish(chunk);
                            });
                            logger.debug("项目 {} 流式分析完成", p);
                        } catch (Exception ex) {
                            String errorMsg = "流式获取失败：" + ex.getMessage() + "\n";
                            publish(errorMsg);
                            logger.error("项目 {} 流式分析失败", p, ex);
                        }

                        // 请求一次非流式完整响应以获得最终 JSON
                        String finalResp = null;
                        try {
                            finalResp = client.analyze(prompt);
                            logger.debug("项目 {} 非流式分析完成", p);
                        } catch (Exception ex) {
                            String errorMsg = "获取最终响应失败：" + ex.getMessage() + "\n";
                            publish(errorMsg);
                            logger.error("项目 {} 非流式分析失败", p, ex);
                        }

                        String toSave = finalResp != null ? finalResp : streamed.toString();
                        logger.debug("项目 {} 响应内容长度: {}", p, toSave.length());

                        // 尝试解析为 JSON，如果成功则美化，否则按原样保存
                        try {
                            //去除对象中的<think>标签
                            toSave= toSave
                                    .replaceAll("<think>[\\s\\S]*?</think>", "")   // 去掉 <think>
                                    .replaceAll("```[a-zA-Z0-9]*", "")
                                    .replaceAll("```", "");
                            toSave = toSave.trim();
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            Object json = mapper.readValue(toSave, Object.class);
                            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            String resultMsg = "\n最终结果（已解析为 JSON）：\n" + pretty + "\n\n";
                            publish(resultMsg);
                            logger.debug("项目 {} 响应为有效JSON格式", p);
                            
                            // 使用正确的构造函数保存结果，确保项目名称和描述能从分析结果中提取
                            AnalysisResult ar = new AnalysisResult(0, p, toSave, java.time.LocalDateTime.now());
                            hm.save(ar);
                            logger.info("项目 {} 分析结果已保存到数据库", p);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            // 不是合法 JSON，保存原始文本
                            String resultMsg = "\n最终结果（非 JSON 文本）：\n" + toSave + "\n\n";
                            publish(resultMsg);
                            logger.debug("项目 {} 响应为非JSON格式", p);
                            
                            // 使用正确的构造函数保存结果，确保项目名称和描述能从分析结果中提取
                            AnalysisResult ar = new AnalysisResult(0, p, toSave, java.time.LocalDateTime.now());
                            hm.save(ar);
                            logger.info("项目 {} 分析结果已保存到数据库", p);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        String errorMsg = "分析失败：" + ex.getMessage() + "\n";
                        publish(errorMsg);
                        logger.error("项目 {} 分析过程中发生错误", p, ex);
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) output.append(s);
            }

            @Override
            protected void done() {
                output.append("分析任务完成。\n");
                logger.info("项目分析任务完成");
            }
        };
        worker.execute();
    }

    private static String buildPromptForProject(File projectDir) {
        logger.debug("开始为项目 {} 构建提示", projectDir.getAbsolutePath());
        
        StringBuilder sb = new StringBuilder();
        // Header with intent and output schema
        sb.append("你是一个代码审查与项目分析助手。不要思考，直接分析下述项目并以严格的 JSON 格式返回结果。不要输出任何额外的文本。JSON 字段说明：\n");
        sb.append("{\"project_name\": \"项目名称\", \"summary\": \"项目概述\", \"modules\": [{\"name\":..., \"description\":...}], \"issues\": [\"...\"], \"suggestions\": [\"...\"], \"top_files\": [\"path\"], \"risk_level\": \"low|medium|high\"}\n");
        sb.append("要求：中文输出；项目名称应该是中文且有意义；摘要不超过200字；modules 不超过10项；issues/suggestions 每项不超过100字；总体不超过2000字。\n");

        // Metadata
        sb.append("项目路径: ").append(projectDir.getAbsolutePath()).append("\n");
        sb.append("检测文件: \n");

        // Include README (first 3000 chars)
        File readme = new File(projectDir, "README.md");
        if (readme.exists()) {
            sb.append("--- README START ---\n");
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(readme))) {
                String line; int chars = 0;
                while ((line = br.readLine()) != null && chars < 3000) {
                    sb.append(line).append("\n");
                    chars += line.length();
                }
            } catch (Exception ignored) {
                logger.warn("读取README文件时发生错误", ignored);
            }
            sb.append("--- README END ---\n");
        }

        // List small selection of important files and sample their content
        java.util.List<String> topFiles = new java.util.ArrayList<>();
        // Look for pom.xml, build.gradle, setup.py, package.json
        File f;
        f = new File(projectDir, "pom.xml"); if (f.exists()) topFiles.add(f.getName());
        f = new File(projectDir, "build.gradle"); if (f.exists()) topFiles.add(f.getName());
        f = new File(projectDir, "package.json"); if (f.exists()) topFiles.add(f.getName());
        f = new File(projectDir, "requirements.txt"); if (f.exists()) topFiles.add(f.getName());
        // Also sample up to 3 source files by extension
        String[] exts = new String[]{"java", "py", "js"};
        int sampled = 0;
        for (String ext : exts) {
            if (sampled >= 3) break;
            File[] arr = projectDir.listFiles((dir, name) -> name.toLowerCase().endsWith("." + ext));
            if (arr == null) continue;
            for (File c : arr) {
                if (sampled >= 3) break;
                topFiles.add(c.getName());
                sb.append("--- FILE: ").append(c.getName()).append(" START ---\n");
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(c))) {
                    String line; int chars = 0;
                    while ((line = br.readLine()) != null && chars < 2000) {
                        sb.append(line).append("\n");
                        chars += line.length();
                    }
                } catch (Exception ignored) {
                    logger.warn("读取文件 {} 时发生错误", c.getName(), ignored);
                }
                sb.append("--- FILE: ").append(c.getName()).append(" END ---\n");
                sampled++;
            }
        }

        sb.append("TopFiles:\n");
        for (String name : topFiles) sb.append(name).append("\n");

        sb.append("注意：不要思考，直接返回必须是单一有效 JSON，且严格遵循上面给出的字段。不要在 JSON 外输出解释或注释。\n");
        sb.append("不要思考，直接按照指定格式输出结果，确保项目名称是中文且有意义。\n");
        
        logger.debug("项目 {} 提示构建完成，总长度: {}", projectDir.getAbsolutePath(), sb.length());
        return sb.toString();
    }
    
    /**
     * 构建用于生成完整使用手册的提示词
     */
    private static String buildManualPromptForProject(File projectDir, String previousAnalysis) {
        logger.debug("开始为项目 {} 构建使用手册提示", projectDir.getAbsolutePath());
        
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个技术文档编写专家。请基于以下项目信息，生成一份完整的使用手册，格式为```\n\n");
        
        sb.append("## 项目信息\n");
        sb.append("项目路径: ").append(projectDir.getAbsolutePath()).append("\n\n");
        
        if (previousAnalysis != null && !previousAnalysis.isEmpty()) {
            sb.append("## 项目分析结果\n");
            sb.append(previousAnalysis).append("\n\n");
        }
        
        sb.append("## 要求\n");
        sb.append("1. 使用```\n``");
        sb.append("2. 手册应包含以下章节：\n");
        sb.append("   - 项目简介\n");
        sb.append("   - 功能特性\n");
        sb.append("   - 安装指南\n");
        sb.append("   - 快速开始\n");
        sb.append("   - 配置说明\n");
        sb.append("   - API文档（如果有）\n");
        sb.append("   - 使用示例\n");
        sb.append("   - 故障排除\n");
        sb.append("   - 常见问题\n");
        sb.append("3. 内容应详实、准确，便于用户理解和使用\n");
        sb.append("4. 使用中文编写\n\n");
        
        sb.append("请直接输出```\n``");
        
        logger.debug("项目 {} 使用手册提示构建完成，总长度: {}", projectDir.getAbsolutePath(), sb.length());
        return sb.toString();
    }

    private static void showSettingsDialog(JFrame parent) {
        logger.debug("显示API设置对话框");
        
        ConfigManager cfg = new ConfigManager();
        JTextField urlField = new JTextField(cfg.get("api.url", cfg.getDefaultApiUrl()), 40);
        JTextField keyField = new JTextField(cfg.get("api.key", "sk-xxx"), 40);
        JTextField modelField = new JTextField(cfg.getModel(), 40);
        
        // 样式化输入框
        styleTextField(urlField);
        styleTextField(keyField);
        styleTextField(modelField);
        
        // 创建标签并设置颜色
        JLabel urlLabel = new JLabel("API 地址 (例如 http://192.168.11.151:8091/v1/chat/completions):");
        urlLabel.setForeground(LABEL_TEXT_COLOR);
        urlLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setForeground(LABEL_TEXT_COLOR);
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        JLabel modelLabel = new JLabel("模型名称 (例如 qwen3-32b-fp8):");
        modelLabel.setForeground(LABEL_TEXT_COLOR);
        modelLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBackground(PANEL_COLOR);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(urlLabel);
        p.add(urlField);
        p.add(keyLabel);
        p.add(keyField);
        p.add(modelLabel);
        p.add(modelField);
        
        JDialog dialog = new JDialog(parent, "设置 API", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(p, BorderLayout.CENTER);
        dialog.setBackground(PANEL_COLOR); // 设置对话框背景色
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton okButton = createStyledButton("确定");
        JButton cancelButton = createStyledButton("取消");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        okButton.addActionListener(e -> {
            cfg.set("api.url", urlField.getText().trim());
            cfg.set("api.key", keyField.getText().trim());
            cfg.setModel(modelField.getText().trim());
            
            logger.info("用户更新API配置 - URL: {}, Model: {}", urlField.getText().trim(), modelField.getText().trim());
            
            try {
                cfg.save();
                JOptionPane.showMessageDialog(dialog, "保存成功");
                logger.info("API配置保存成功");
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "保存失败：" + ex.getMessage());
                logger.error("API配置保存失败", ex);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
    
    /**
     * 样式化文本输入框
     */
    private static void styleTextField(JTextField textField) {
        textField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        textField.setForeground(TEXT_COLOR); // 设置文字颜色
        textField.setBackground(Color.WHITE); // 设置背景颜色
        textField.setCaretColor(TEXT_COLOR); // 设置光标颜色
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    private static void showHistoryDialog(JFrame parent) {
        logger.debug("显示历史记录对话框");
        
        HistoryManager hm = new HistoryManager();
        java.util.List<AnalysisResult> items = hm.list();

        // 更新列定义以包含项目名称和描述
        String[] cols = new String[] { "ID", "Project Name", "Project Description", "Analyzed At" };
        Object[][] data = new Object[items.size()][cols.length];
        for (int i = 0; i < items.size(); i++) {
            AnalysisResult ar = items.get(i);
            data[i][0] = ar.id;
            data[i][1] = ar.projectName;
            data[i][2] = ar.projectDescription;
            data[i][3] = ar.analyzedAt.toString();
        }

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(800, 300));
        styleTable(table);

        JDialog dlg = new JDialog(parent, "历史分析记录", true);
        dlg.setLayout(new BorderLayout());
        dlg.setBackground(PANEL_COLOR); // 设置对话框背景色
        
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setBackground(PANEL_COLOR);
        dlg.add(tableScrollPane, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(BACKGROUND_COLOR);
        JButton refresh = createStyledButton("刷新");
        JButton details = createStyledButton("查看详情");
        JButton generateManual = createStyledButton("生成使用手册");
        JButton openFolder = createStyledButton("打开文件夹");
        JButton close = createStyledButton("关闭");
        btns.add(refresh);
        btns.add(details);
        btns.add(generateManual);
        btns.add(openFolder);
        btns.add(close);
        dlg.add(btns, BorderLayout.SOUTH);

        refresh.addActionListener(ev -> {
            logger.debug("用户点击刷新历史记录");
            
            java.util.List<AnalysisResult> newItems = hm.list();
            Object[][] newData = new Object[newItems.size()][cols.length];
            for (int i = 0; i < newItems.size(); i++) {
                AnalysisResult ar = newItems.get(i);
                newData[i][0] = ar.id;
                newData[i][1] = ar.projectName;
                newData[i][2] = ar.projectDescription;
                newData[i][3] = ar.analyzedAt.toString();
            }
            model.setDataVector(newData, cols);
            
            logger.debug("历史记录刷新完成，共 {} 条记录", newItems.size());
        });

        details.addActionListener(ev -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(dlg, "请选择一行以查看详情");
                logger.warn("用户尝试查看详情但未选择任何记录");
                return;
            }
            int id = Integer.parseInt(String.valueOf(model.getValueAt(sel, 0)));
            try {
                // 查询单条记录并显示详情
                java.util.List<AnalysisResult> list = hm.list();
                AnalysisResult found = null;
                for (AnalysisResult ar : list) if (ar.id == id) { found = ar; break; }
                if (found == null) {
                    JOptionPane.showMessageDialog(dlg, "未找到记录");
                    logger.warn("未找到ID为 {} 的历史记录", id);
                    return;
                }
                
                // 尝试解析为结构化数据显示，如果失败则显示原始详情
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.readTree(found.result);
                    // 如果能成功解析为JSON，则使用结构化显示
                    showStructuredDialog(dlg, found.result);
                    logger.debug("显示ID为 {} 的结构化历史记录", id);
                } catch (Exception ex) {
                    // 如果不是有效的JSON，则使用普通详情显示
                    showDetailsDialog(dlg, found.result);
                    logger.debug("显示ID为 {} 的原始历史记录", id);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "获取详情失败：" + ex.getMessage());
                logger.error("获取历史记录详情失败", ex);
            }
        });

        // 添加生成使用手册功能
        generateManual.addActionListener(ev -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(dlg, "请选择一行以生成使用手册");
                logger.warn("用户尝试生成使用手册但未选择任何记录");
                return;
            }
            
            try {
                // 获取选中行的项目信息
                java.util.List<AnalysisResult> list = hm.list();
                int id = Integer.parseInt(String.valueOf(model.getValueAt(sel, 0)));
                AnalysisResult found = null;
                for (AnalysisResult ar : list) if (ar.id == id) { found = ar; break; }
                
                if (found == null) {
                    JOptionPane.showMessageDialog(dlg, "未找到记录");
                    logger.warn("未找到ID为 {} 的历史记录", id);
                    return;
                }
                
                // 开始生成使用手册
                generateProjectManual(dlg, found);
                logger.info("开始为项目 {} 生成使用手册", found.projectPath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "生成使用手册失败：" + ex.getMessage());
                logger.error("生成使用手册失败", ex);
            }
        });

        // 添加打开文件夹功能
        openFolder.addActionListener(ev -> {
            int sel = table.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(dlg, "请选择一行以打开文件夹");
                logger.warn("用户尝试打开文件夹但未选择任何记录");
                return;
            }
            
            try {
                // 获取选中行的项目路径
                java.util.List<AnalysisResult> list = hm.list();
                int id = Integer.parseInt(String.valueOf(model.getValueAt(sel, 0)));
                AnalysisResult found = null;
                for (AnalysisResult ar : list) if (ar.id == id) { found = ar; break; }
                
                if (found == null) {
                    JOptionPane.showMessageDialog(dlg, "未找到记录");
                    logger.warn("未找到ID为 {} 的历史记录", id);
                    return;
                }
                
                // 打开文件夹
                openFolderInSystem(found.projectPath);
                logger.info("打开文件夹: {}", found.projectPath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "打开文件夹失败：" + ex.getMessage());
                logger.error("打开文件夹失败", ex);
            }
        });

        close.addActionListener(ev -> {
            dlg.dispose();
            logger.debug("关闭历史记录对话框");
        });

        dlg.pack();
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
    
    /**
     * 样式化表格
     */
    private static void styleTable(JTable table) {
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(25);
        table.setSelectionBackground(new Color(173, 216, 230));
        table.setSelectionForeground(TEXT_COLOR); // 设置选中行文字颜色
        table.setBackground(PANEL_COLOR); // 设置表格背景颜色
        table.setForeground(TEXT_COLOR); // 设置表格文字颜色
        
        // 设置表格单元格渲染器以确保颜色正确显示
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        });
    }
    
    /**
     * 在系统中打开指定路径的文件夹
     * @param folderPath 文件夹路径
     */
    private static void openFolderInSystem(String folderPath) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                logger.warn("文件夹不存在: {}", folderPath);
                JOptionPane.showMessageDialog(null, "文件夹不存在: " + folderPath);
                return;
            }
            
            if (!folder.isDirectory()) {
                logger.warn("路径不是一个文件夹: {}", folderPath);
                JOptionPane.showMessageDialog(null, "路径不是一个文件夹: " + folderPath);
                return;
            }
            
            // 根据操作系统选择合适的命令
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows系统
                Runtime.getRuntime().exec("explorer.exe " + folder.getAbsolutePath());
            } else if (os.contains("mac")) {
                // macOS系统
                Runtime.getRuntime().exec("open " + folder.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux系统
                Runtime.getRuntime().exec("xdg-open " + folder.getAbsolutePath());
            } else {
                logger.warn("不支持的操作系统: {}", os);
                JOptionPane.showMessageDialog(null, "不支持在此操作系统上打开文件夹: " + os);
            }
        } catch (Exception e) {
            logger.error("打开文件夹时发生错误: {}", folderPath, e);
            JOptionPane.showMessageDialog(null, "打开文件夹失败: " + e.getMessage());
        }
    }
    
    private static void showDetailsDialog(Component parent, String analysisJson) {
        logger.debug("显示详情对话框");
        
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), "分析详情", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setBackground(PANEL_COLOR); // 设置对话框背景色
        
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        ta.setForeground(TEXT_COLOR); // 设置文字颜色
        ta.setBackground(PANEL_COLOR); // 设置背景颜色
        ta.setCaretColor(TEXT_COLOR); // 设置光标颜色
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object obj = mapper.readValue(analysisJson, Object.class);
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            ta.setText(pretty);
        } catch (Exception e) {
            ta.setText(analysisJson);
        }
        JScrollPane scrollPane = new JScrollPane(ta);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dlg.add(scrollPane, BorderLayout.CENTER);
        JButton close = createStyledButton("关闭");
        close.addActionListener(ev -> dlg.dispose());
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        p.setBackground(BACKGROUND_COLOR);
        p.add(close);
        dlg.add(p, BorderLayout.SOUTH);
        dlg.setSize(800, 600);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private static void showStructuredDialog(Component parent, String analysisJson) {
        logger.debug("显示结构化结果对话框");
        
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), "结构化分析结果", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setBackground(PANEL_COLOR); // 设置对话框背景色

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root;
        try {
            root = mapper.readTree(analysisJson);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dlg, "不是合法 JSON：" + e.getMessage());
            logger.error("解析JSON失败", e);
            return;
        }

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(PANEL_COLOR);
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Summary
        String summary = root.path("summary").asText("");
        JTextArea sumArea = new JTextArea(summary);
        sumArea.setLineWrap(true);
        sumArea.setWrapStyleWord(true);
        sumArea.setEditable(false);
        sumArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        sumArea.setForeground(TEXT_COLOR); // 设置文字颜色
        sumArea.setBackground(PANEL_COLOR); // 设置背景颜色
        sumArea.setCaretColor(TEXT_COLOR); // 设置光标颜色
        sumArea.setBorder(BorderFactory.createTitledBorder("摘要"));
        sumArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("摘要"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        main.add(new JScrollPane(sumArea));

        // Modules (table)
        if (root.has("modules") && root.get("modules").isArray()) {
            java.util.List<com.fasterxml.jackson.databind.JsonNode> mods = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode n : root.get("modules")) mods.add(n);
            String[] cols = new String[] {"Name", "Description"};
            Object[][] data = new Object[mods.size()][2];
            for (int i = 0; i < mods.size(); i++) {
                data[i][0] = mods.get(i).path("name").asText();
                data[i][1] = mods.get(i).path("description").asText();
            }
            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, cols) {
                public boolean isCellEditable(int row, String[]  col) { return false; }
            };
            JTable table = new JTable(model);
            table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            table.setForeground(TEXT_COLOR); // 设置文字颜色
            table.setBackground(PANEL_COLOR); // 设置背景颜色
            table.setPreferredScrollableViewportSize(new Dimension(700, 120));
            JPanel pMods = new JPanel(new BorderLayout());
            pMods.setBackground(PANEL_COLOR);
            pMods.setBorder(BorderFactory.createTitledBorder("模块 (Modules)"));
            pMods.add(new JScrollPane(table), BorderLayout.CENTER);
            main.add(pMods);
        }

        // Issues and Suggestions
        JPanel pIS = new JPanel(new GridLayout(1,2, 10, 0));
        pIS.setBackground(PANEL_COLOR);
        // Issues
        java.util.List<String> issues = new java.util.ArrayList<>();
        if (root.has("issues") && root.get("issues").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode n : root.get("issues")) issues.add(n.asText());
        }
        JList<String> issuesList = new JList<>(issues.toArray(new String[0]));
        issuesList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        issuesList.setForeground(TEXT_COLOR); // 设置文字颜色
        issuesList.setBackground(PANEL_COLOR); // 设置背景颜色
        issuesList.setSelectionForeground(TEXT_COLOR); // 设置选中文字颜色
        issuesList.setSelectionBackground(new Color(173, 216, 230)); // 设置选中背景颜色
        JScrollPane issuesPane = new JScrollPane(issuesList);
        issuesPane.getViewport().setBackground(PANEL_COLOR);
        issuesPane.setBorder(BorderFactory.createTitledBorder("问题 (Issues)"));
        pIS.add(issuesPane);

        // Suggestions
        java.util.List<String> suggs = new java.util.ArrayList<>();
        if (root.has("suggestions") && root.get("suggestions").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode n : root.get("suggestions")) suggs.add(n.asText());
        }
        JList<String> suggList = new JList<>(suggs.toArray(new String[0]));
        suggList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        suggList.setForeground(TEXT_COLOR); // 设置文字颜色
        suggList.setBackground(PANEL_COLOR); // 设置背景颜色
        suggList.setSelectionForeground(TEXT_COLOR); // 设置选中文字颜色
        suggList.setSelectionBackground(new Color(173, 216, 230)); // 设置选中背景颜色
        JScrollPane suggPane = new JScrollPane(suggList);
        suggPane.getViewport().setBackground(PANEL_COLOR);
        suggPane.setBorder(BorderFactory.createTitledBorder("建议 (Suggestions)"));
        pIS.add(suggPane);

        main.add(pIS);

        // Top files and risk
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(PANEL_COLOR);
        java.util.List<String> topFiles = new java.util.ArrayList<>();
        if (root.has("top_files") && root.get("top_files").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode n : root.get("top_files")) topFiles.add(n.asText());
        }
        JList<String> tfList = new JList<>(topFiles.toArray(new String[0]));
        tfList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        tfList.setForeground(TEXT_COLOR); // 设置文字颜色
        tfList.setBackground(PANEL_COLOR); // 设置背景颜色
        tfList.setSelectionForeground(TEXT_COLOR); // 设置选中文字颜色
        tfList.setSelectionBackground(new Color(173, 216, 230)); // 设置选中背景颜色
        tfList.setBorder(BorderFactory.createTitledBorder("Top Files"));
        bottom.add(new JScrollPane(tfList), BorderLayout.CENTER);

        String risk = root.path("risk_level").asText("");
        JLabel riskLbl = new JLabel("风险等级: " + risk);
        riskLbl.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        riskLbl.setForeground(TEXT_COLOR); // 设置文字颜色
        JPanel pr = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pr.setBackground(PANEL_COLOR);
        pr.add(riskLbl);
        bottom.add(pr, BorderLayout.SOUTH);

        main.add(bottom);

        JScrollPane scrollPane = new JScrollPane(main);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        dlg.add(scrollPane, BorderLayout.CENTER);
        JButton close2 = createStyledButton("关闭");
        close2.addActionListener(ev -> dlg.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bp.setBackground(BACKGROUND_COLOR);
        bp.add(close2);
        dlg.add(bp, BorderLayout.SOUTH);
        dlg.setSize(900, 700);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
    
    /**
     * 生成项目使用手册
     */
    private static void generateProjectManual(Component parent, AnalysisResult analysisResult) {
        logger.debug("开始生成项目使用手册: {}", analysisResult.projectPath);
        
        // 显示进度对话框
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog progressDlg;
        if (window instanceof Frame) {
            progressDlg = new JDialog((Frame) window, "生成使用手册", true);
        } else if (window instanceof Dialog) {
            progressDlg = new JDialog((Dialog) window, "生成使用手册", true);
        } else {
            progressDlg = new JDialog((Frame) null, "生成使用手册", true);
        }
        
        progressDlg.setLayout(new BorderLayout());
        progressDlg.setSize(300, 100);
        progressDlg.setLocationRelativeTo(parent);
        
        JLabel progressLabel = new JLabel("正在生成使用手册，请稍候...", JLabel.CENTER);
        progressLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        progressLabel.setForeground(TEXT_COLOR);
        progressDlg.add(progressLabel, BorderLayout.CENTER);
        
        // 在新线程中执行生成操作
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    ConfigManager cfg = new ConfigManager();
                    String apiUrl = cfg.get("api.url", System.getenv().getOrDefault("OPENAI_API_URL", cfg.getDefaultApiUrl()));
                    String apiKey = cfg.get("api.key", System.getenv().getOrDefault("OPENAI_API_KEY", "sk-xxx"));
                    String model = cfg.getModel();
                    
                    if (apiUrl.isEmpty() || apiKey.isEmpty()) {
                        throw new Exception("未设置 API 地址或 Key");
                    }
                    
                    LLMClient client = new LLMClient(apiUrl, apiKey, model);
                    File projectDir = new File(analysisResult.projectPath);
                    
                    // 构建生成手册的提示词
                    String prompt = buildManualPromptForProject(projectDir, analysisResult.result);
                    logger.debug("使用手册提示词构建完成，长度: {}", prompt.length());
                    
                    // 调用大模型生成使用手册
                    String manual = client.analyze(prompt);
                    logger.info("使用手册生成完成，长度: {}", manual.length());
                    
                    return manual;
                } catch (Exception e) {
                    logger.error("生成使用手册失败", e);
                    throw e;
                }
            }

            @Override
            protected void done() {
                progressDlg.dispose();
                
                try {
                    String manual = get();
                    // 显示生成的使用手册
                    showManualDialog(parent, analysisResult.projectName, manual);
                    logger.info("项目 {} 的使用手册生成并显示成功", analysisResult.projectPath);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(parent), 
                        "生成使用手册失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    logger.error("显示使用手册失败", e);
                }
            }
        };
        
        worker.execute();
        progressDlg.setVisible(true);
    }
    
    /**
     * 显示生成的使用手册
     */
    private static void showManualDialog(Component parent, String projectName, String manual) {
        logger.debug("显示使用手册对话框");
        
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), "使用手册 - " + projectName, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        dlg.setBackground(PANEL_COLOR); // 设置对话框背景色
        
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        ta.setText(manual);
        ta.setForeground(TEXT_COLOR);
        ta.setBackground(PANEL_COLOR);
        ta.setCaretColor(TEXT_COLOR); // 设置光标颜色
        
        JScrollPane scrollPane = new JScrollPane(ta);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dlg.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        
        JButton saveButton = createStyledButton("保存到文件");
        JButton closeButton = createStyledButton("关闭");
        
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(projectName + "_使用手册.md"));
            int result = fileChooser.showSaveDialog(dlg);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(manual);
                    JOptionPane.showMessageDialog(dlg, "使用手册已保存到: " + file.getAbsolutePath());
                    logger.info("使用手册已保存到: {}", file.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "保存失败：" + ex.getMessage());
                    logger.error("保存使用手册失败", ex);
                }
            }
        });
        
        closeButton.addActionListener(e -> dlg.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        dlg.add(buttonPanel, BorderLayout.SOUTH);
        
        dlg.setSize(900, 700);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
}