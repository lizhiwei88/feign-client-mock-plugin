package com.github.lizhiwei88.feign.mock.plugin.patcher;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * FeignAgentPatcher
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
public class FeignAgentPatcher extends JavaProgramPatcher {

    private static final Logger log = Logger.getInstance(FeignAgentPatcher.class);

    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        // 1. 正确获取 Project 实例
        Project project = null;
        if (configuration instanceof RunConfiguration runConfiguration) {
            project = runConfiguration.getProject();
        }

        if (project == null) return;

        // 2. 提取并获取 Agent Jar 的物理路径
        String agentJarPath = getAgentJarPath();
        if (agentJarPath == null) return;

        // 3. 构造 JavaAgent 参数
        // 格式：-javaagent:/path/to/agent.jar
        javaParameters.getVMParametersList().add("-javaagent:" + agentJarPath);

        // 4. [关键] 将 Agent Jar 也加入 Classpath
        // 这样 Spring 的 ClassLoader 才能看到我们定义的 ApplicationContextInitializer
        javaParameters.getClassPath().add(agentJarPath);

        // 5. 针对 JDK 17+ (Spring Boot 3) 开启强行反射访问
        addOpensForJava17(javaParameters);

        log.info("[Feign-Mock-Plugin] Successfully patched agent to: " + configuration.getName());
    }

    /**
     * 将插件内置的 Agent Jar 拷贝到临时目录，并返回路径
     */
    private String getAgentJarPath() {
        try {
            // 在 IDEA 的插件配置目录创建一个临时文件
            File pluginDir = new File(PathManager.getPluginsPath(), "feign-mock-helper");
            if (!pluginDir.exists()) pluginDir.mkdirs();

            File agentJar = new File(pluginDir, "feign-client-mock-agent.jar");

            // 每次启动都尝试覆盖，确保 Agent 是最新版
            try (InputStream is = getClass().getResourceAsStream("/bin/feign-client-mock-agent-1.0.0.jar");
                 FileOutputStream os = new FileOutputStream(agentJar)) {
                if (is == null) return null;
                FileUtil.copy(is, os);
            }
            return agentJar.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addOpensForJava17(JavaParameters javaParameters) {
        Sdk sdk = javaParameters.getJdk();
        if (sdk != null) {
            JavaSdkVersion version = JavaSdkVersion.fromVersionString(sdk.getVersionString());
            if (version != null && version.isAtLeast(JavaSdkVersion.JDK_17)) {
                javaParameters.getVMParametersList().add("--add-opens=java.base/java.lang=ALL-UNNAMED");
                javaParameters.getVMParametersList().add("--add-opens=java.base/java.util=ALL-UNNAMED");
            }
        }
    }
}