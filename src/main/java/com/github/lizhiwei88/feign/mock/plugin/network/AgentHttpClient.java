package com.github.lizhiwei88.feign.mock.plugin.network;

import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockSettings;
import com.intellij.openapi.project.Project;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * AgentHttpClient
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
public class AgentHttpClient {

    // 配置超时时间 (单位: 毫秒)
    // ConnectTimeout: 2秒连接超时
    // SocketTimeout: 3秒读取超时 (防止应用在断点或处理慢时卡住插件后台线程)
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(2000)
            .setConnectionRequestTimeout(2000)
            .setSocketTimeout(3000)
            .build();

    /**
     * 发送 update 请求（POST）
     */
    public static String sendUpdate(Project project, String signature, String json) {
        return sendPost(project, "update", signature, json);
    }

    /**
     * 发送 delete 请求（POST）
     */
    public static String sendClear(Project project, String signature) {
        return sendPost(project, "delete", signature, "");
    }

    /**
     * 发送 ping 请求（GET）
     */
    public static String sendPing(Project project) {
        return sendGet(project, "ping");
    }

    private static String sendPost(Project project, String api, String signature, String json) {
        int port = getPort(project);
        if (port == -1) return "Spring Boot application not started.";
        String url = "http://localhost:" + port + "/" + api;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setConfig(REQUEST_CONFIG);
            String cleanedJson = cleanJsonFormatting(json);
            String payload = String.format("{\"methodSignature\":\"%s\", \"json\":\"%s\"}", signature, cleanedJson == null ? "" : cleanedJson.replace("\"", "\\\""));
            post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));
            post.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String sendGet(Project project, String api) {
        int port = getPort(project);
        if (port == -1) return "Spring Boot application not started.";
        String url = "http://localhost:" + port + "/" + api;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setConfig(REQUEST_CONFIG);
            try (CloseableHttpResponse response = client.execute(get)) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    private static int getPort(Project project) {
        return Objects.requireNonNull(FeignMockSettings.getInstance(project).getState()).lastKnownPort;
    }

    /**
     * 清理 JSON 中多余的空白字符
     */
    private static String cleanJsonFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", " ").trim();
    }
}