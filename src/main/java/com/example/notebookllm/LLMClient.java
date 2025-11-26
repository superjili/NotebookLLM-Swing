package com.example.notebookllm;

import okhttp3.*;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 添加日志导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LLMClient {
    // 添加日志实例
    private static final Logger logger = LoggerFactory.getLogger(LLMClient.class);
    
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public LLMClient(String apiUrl, String apiKey) {
        this(apiUrl, apiKey, "gpt-3.5-turbo");
    }

    public LLMClient(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        logger.info("初始化LLM客户端 - API URL: {}, Model: {}", apiUrl, model);
    }

    public String analyze(String prompt) throws IOException {
        logger.debug("开始非流式分析，提示长度: {}", prompt.length());
        
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String bodyJson = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]}";
        RequestBody body = RequestBody.create(bodyJson, mediaType);
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
                
        logger.debug("发送HTTP请求到: {}", apiUrl);
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("HTTP请求失败，状态码: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            
            String respBody = response.body().string();
            logger.debug("收到HTTP响应，响应体长度: {}", respBody.length());
            
            // 尝试解析常见 OpenAI 响应结构
            try {
                JsonNode root = mapper.readTree(respBody);
                if (root.has("choices")) {
                    JsonNode choices = root.get("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode first = choices.get(0);
                        if (first.has("message") && first.get("message").has("content")) {
                            String result = first.get("message").get("content").asText();
                            logger.debug("成功解析响应内容，结果长度: {}", result.length());
                            return result;
                        }
                        if (first.has("text")) {
                            String result = first.get("text").asText();
                            logger.debug("成功解析响应内容，结果长度: {}", result.length());
                            return result;
                        }
                    }
                }
            } catch (Exception ignored) {
                logger.warn("解析响应结构时发生异常", ignored);
            }
            // 如果不能解析，返回原始响应
            logger.debug("返回原始响应内容，长度: {}", respBody.length());
            return respBody;
        }
    }

    /**
     * 支持流式返回。会在当前线程同步阻塞读取流，并在接收到每个 content 片段时调用 onChunk。
     * 适用于 OpenAI Chat Completions 的 stream=true 返回格式（data: {...} 每行）。
     */
    public void analyzeStream(String prompt, java.util.function.Consumer<String> onChunk) throws IOException {
        logger.debug("开始流式分析，提示长度: {}", prompt.length());
        
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String bodyJson = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}],\"stream\":true}";
        RequestBody body = RequestBody.create(bodyJson, mediaType);
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        logger.debug("发送流式HTTP请求到: {}", apiUrl);
                
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("流式HTTP请求失败，状态码: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            
            okhttp3.ResponseBody rb = response.body();
            if (rb == null) {
                logger.warn("响应体为空");
                return;
            }
            
            try (okio.BufferedSource src = rb.source()) {
                int chunkCount = 0;
                while (!src.exhausted()) {
                    String line;
                    try {
                        line = src.readUtf8Line();
                    } catch (Exception e) {
                        logger.error("读取响应流时发生异常", e);
                        break;
                    }
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    // OpenAI stream lines begin with "data: "
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            logger.debug("收到流结束标记[DONE]");
                            break;
                        }
                        try {
                            JsonNode root = mapper.readTree(data);
                            if (root.has("choices")) {
                                JsonNode choices = root.get("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode first = choices.get(0);
                                    // Chat Completions v1 stream uses delta.content
                                    if (first.has("delta") && first.get("delta").has("content")) {
                                        String c = first.get("delta").get("content").asText();
                                        if (c != null && !c.isEmpty()) {
                                            onChunk.accept(c);
                                            chunkCount++;
                                        }
                                    } else if (first.has("message") && first.get("message").has("content")) {
                                        String c = first.get("message").get("content").asText();
                                        if (c != null && !c.isEmpty()) {
                                            onChunk.accept(c);
                                            chunkCount++;
                                        }
                                    } else if (first.has("text")) {
                                        String c = first.get("text").asText();
                                        if (c != null && !c.isEmpty()) {
                                            onChunk.accept(c);
                                            chunkCount++;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            // 不再转发原始数据，避免混乱
                            // 记录日志或静默处理异常
                            logger.warn("解析流数据块时发生异常: {}", data, ex);
                        }
                    }
                    // 忽略非 data 行，不再传递给 onChunk
                }
                logger.debug("流式分析完成，共处理 {} 个数据块", chunkCount);
            }
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}