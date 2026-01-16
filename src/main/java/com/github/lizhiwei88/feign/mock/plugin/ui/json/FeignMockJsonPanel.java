package com.github.lizhiwei88.feign.mock.plugin.ui.json;

import com.github.lizhiwei88.feign.mock.plugin.common.FeignMockRuntimeManager;
import com.github.lizhiwei88.feign.mock.plugin.common.StartupStatus;
import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.github.lizhiwei88.feign.mock.plugin.network.AgentRequestManager;
import com.github.lizhiwei88.feign.mock.plugin.notifications.NotificationUtil;
import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Objects;

/**
 * JsonPanel
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/30
 */
public class FeignMockJsonPanel extends JBPanel<FeignMockJsonPanel> {

    private static final Logger log = Logger.getInstance(FeignMockJsonPanel.class);

    private final JsonEditorPanel jsonEditorPanel;

    private final transient Project project;

    private final JButton applyButton;

    private final JButton cleanButton;

    private final Tree feignTree;

    public FeignMockJsonPanel(Project project, Tree feignTree) {
        super(new BorderLayout());
        this.project = project;
        this.feignTree = feignTree;
        jsonEditorPanel = new JsonEditorPanel(project);
        this.applyButton = new JButton("Apply");
        this.cleanButton = new JButton("Clean");
        this.applyButton.setEnabled(false);
        this.cleanButton.setEnabled(false);
        cleanButton.setEnabled(false);
        JBPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(applyButton);
        buttonPanel.add(cleanButton);
        add(jsonEditorPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        // 设置默认按钮
        SwingUtilities.invokeLater(() -> {
            JRootPane root = buttonPanel.getRootPane(); if (root != null) root.setDefaultButton(applyButton); });
        // 监听json编辑器内容变化
        jsonEditorPanel.getJsonEditor().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) feignTree.getLastSelectedPathComponent();
                // 如果不是FeignMethodNode节点 按钮全部禁用
                if (selectedNode == null || !(selectedNode.getUserObject() instanceof FeignMethodNode nodeData)) {
                    applyButton.setEnabled(false);
                    cleanButton.setEnabled(false);
                    return;
                }
                updateButtonStatus(nodeData.getMockData(), jsonEditorPanel.getJsonEditor().getText());
            }
        });
        // 监听按钮点击
        applyButton.addActionListener(e -> processJsonText(false));
        cleanButton.addActionListener(e -> processJsonText(true));
    }

    public JsonEditorPanel getJsonEditorPanel() {
        return jsonEditorPanel;
    }

    private void processJsonText(boolean cleanFlag) {
        // 1.获取json数据
        String json = null;
        if (!cleanFlag) {
            json = jsonEditorPanel.getJsonEditor().getText();
        }
        final String finalJson = json;

        // 2.获取当前tree节点
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) feignTree.getLastSelectedPathComponent();
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof FeignMethodNode nodeData)) {
            return;
        }

        // 3. 处理请求逻辑 (异步化)
        if (Objects.isNull(finalJson) || finalJson.isBlank()) {
            if (StringUtils.isNotBlank(nodeData.getMockData())) {
                FeignMockData.getInstance(project).remove(nodeData.getSignature());

                if (FeignMockRuntimeManager.getInstance(project).getStatus() == StartupStatus.RUNNING) {
                    new Task.Backgroundable(project, "Clearing mock data...", true) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            log.info("发送清除请求中...");
                            String result = AgentRequestManager.sendClear(project, nodeData.getSignature());
                            log.info("发送结果: " + result);

                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (!Objects.equals("Deleted", result) && !AgentRequestManager.STATUS_SUSPENDED.equals(result)) {
                                    NotificationUtil.showError(project, "Apply failed!");
                                } else {
                                    // 成功或挂起后更新UI
                                    updateUiAfterClear(nodeData);
                                }
                            });
                        }
                    }.queue();
                    return; // 异步执行，直接返回
                }
            }
            // 如果不需要发请求(没启动或本地也没数据)，直接更新UI
            updateUiAfterClear(nodeData);
            return;
        }

        // 更新数据请求
        FeignMockData.getInstance(project).put(nodeData.getSignature(), finalJson);
        if (FeignMockRuntimeManager.getInstance(project).getStatus() == StartupStatus.RUNNING) {
            new Task.Backgroundable(project, "Updating mock data...", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    log.info("发送更新请求中...");
                    String result = AgentRequestManager.sendUpdate(project, nodeData.getSignature(), finalJson);
                    log.info("发送结果: " + result);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!Objects.equals("OK", result) && !AgentRequestManager.STATUS_SUSPENDED.equals(result)) {
                            NotificationUtil.showError(project, "Apply failed!");
                        } else {
                            // 成功或挂起后更新UI
                            updateUiAfterUpdate(nodeData, finalJson);
                        }
                    });
                }
            }.queue();
            return; // 异步执行
        }

        // 如果服务没启动，直接更新UI
        updateUiAfterUpdate(nodeData, finalJson);
    }

    private void updateUiAfterClear(FeignMethodNode nodeData) {
        jsonEditorPanel.getJsonEditor().setText(null);
        nodeData.setMockData(null);
        updateButtonStatus(nodeData.getMockData(), null);
        feignTree.repaint();
    }

    private void updateUiAfterUpdate(FeignMethodNode nodeData, String json) {
        nodeData.setMockData(json);
        updateButtonStatus(nodeData.getMockData(), json);
        feignTree.repaint();
    }

    private void updateButtonStatus(String mockData, String jsonText) {
        if (StringUtils.isBlank(jsonText) && StringUtils.isBlank(mockData)) {
            applyButton.setEnabled(false);
            cleanButton.setEnabled(false);
            return;
        }
        // 如果json内容和树节点数据一致 按钮全部禁用
        applyButton.setEnabled(!Objects.equals(jsonText, mockData));
        cleanButton.setEnabled(!jsonText.isEmpty());
    }
}
