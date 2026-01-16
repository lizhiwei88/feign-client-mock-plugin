package com.github.lizhiwei88.feign.mock.plugin.common;

import com.github.lizhiwei88.feign.mock.plugin.network.AgentRequestManager;
import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockData;
import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockSettingFileReload;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 运行时管理
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/30
 */
@Service(Service.Level.PROJECT)
public final class FeignMockRuntimeManager {

    private static final Logger log = Logger.getInstance(FeignMockRuntimeManager.class);
    private volatile StartupStatus status = StartupStatus.STOPPED;
    private final Project project;

    public FeignMockRuntimeManager(Project project) {
        this.project = project;
    }

    public static FeignMockRuntimeManager getInstance(@NotNull Project project) {
        return project.getService(FeignMockRuntimeManager.class);
    }

    public StartupStatus getStatus() {
        return status;
    }

    public void setStatus(StartupStatus status) {
        this.status = status;
    }

    /**
     * 在后台线程执行的监控逻辑
     *
     * @param indicator
     */
    public void startMonitoring(ProgressIndicator indicator) {
        setStatus(StartupStatus.STARTING);
        int maxRetry = 200;
        for (int i = 0; i < maxRetry; i++) {
            if (indicator != null && indicator.isCanceled()) {
                setStatus(StartupStatus.STOPPED);
                return;
            }
            if (getStatus() == StartupStatus.STOPPED) {
                return;
            }

            // 每隔5次重新加载配置
            if (i % 5 == 0) {
                FeignMockSettingFileReload.parseExternalXml(project);
            }

            log.info("等待服务启动中... (" + (i + 1) + "/" + maxRetry + ")");
            String result = AgentRequestManager.sendPing(project);
            log.info("服务状态: " + result);

            if ("pong".equals(result)) {
                log.info("服务启动成功");
                setStatus(StartupStatus.RUNNING);
                pushConfig();
                return;
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                setStatus(StartupStatus.STOPPED);
                Thread.currentThread().interrupt();
                return;
            }
        }
        // 超时未启动
        setStatus(StartupStatus.STOPPED);
    }

    public void stopMonitoring() {
        setStatus(StartupStatus.STOPPED);
    }

    private void pushConfig() {
        FeignMockData feignMockData = FeignMockData.getInstance(project);
        if (feignMockData == null) return;

        feignMockData.getAll().forEach((signature, json) -> {
            if (json == null || json.isBlank()) {
                return;
            }
            log.debug("发送数据中...");
            String result = AgentRequestManager.sendUpdate(project, signature, json);
            log.debug("发送结果: " + result);
        });
    }
}
