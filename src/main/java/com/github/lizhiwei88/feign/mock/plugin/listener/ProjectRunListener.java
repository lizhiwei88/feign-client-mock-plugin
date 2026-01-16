package com.github.lizhiwei88.feign.mock.plugin.listener;

import com.github.lizhiwei88.feign.mock.plugin.common.FeignMockRuntimeManager;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

/**
 * 项目运行监听器
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/6
 */
public class ProjectRunListener implements ExecutionListener {

    private static final Logger log = Logger.getInstance(ProjectRunListener.class);

    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        log.info("项目开始运行: " + env.getRunProfile().getName());

        // 尝试关联 Debug Session
        try {
            XDebuggerManager manager = XDebuggerManager.getInstance(env.getProject());
            if (manager != null) {
                for (XDebugSession session : manager.getDebugSessions()) {
                    if (session.getRunContentDescriptor().getProcessHandler() == handler) {
                        FeignMockDebugListener.attach(session);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
            // 忽略可能的类缺失或非Debug环境错误
        }

        // 启动后台线程轮询 ping 接口
        // 使用后台任务加载数据
        new Task.Backgroundable(env.getProject(), "Monitor service...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                FeignMockRuntimeManager.getInstance(env.getProject()).startMonitoring(indicator);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                log.error("Monitoring task failed", error);
            }
        }.queue();

    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        log.info("项目运行结束: " + env.getRunProfile().getName());
        FeignMockRuntimeManager.getInstance(env.getProject()).stopMonitoring();
    }
}
