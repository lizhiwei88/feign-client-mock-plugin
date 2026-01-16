package com.github.lizhiwei88.feign.mock.plugin.common;

/**
 * 启动状态枚举
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/7
 */
public enum StartupStatus {

    STARTING("启动中"),
    RUNNING("运行中"),
    STOPPED("已停止");

    private final String description;

    StartupStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
