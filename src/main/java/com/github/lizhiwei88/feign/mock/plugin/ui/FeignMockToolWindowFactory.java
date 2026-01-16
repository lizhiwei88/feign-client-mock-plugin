package com.github.lizhiwei88.feign.mock.plugin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * FeignMockToolWindowFactory 工具窗口工厂
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
public class FeignMockToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 获取内容工厂
        ContentFactory contentFactory = ContentFactory.getInstance();

        // 创建主标签页
        FeignMockTab launchParamTab = new FeignMockTab(project);
        Content launchParamContent = contentFactory.createContent(launchParamTab, "", false);

        // 将所有标签页添加到工具窗口
        toolWindow.getContentManager().addContent(launchParamContent);

        // Add GitHub link to the tool window title actions
        toolWindow.setTitleActions(List.of(
                new AnAction("Refresh", "Refresh feign mock data", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        launchParamTab.refreshTree();
                    }
                },
                new AnAction("GitHub", "Visit GitHub repository", AllIcons.Vcs.Vendors.Github) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        BrowserUtil.browse("https://github.com/lizhiwei88/feign-client-mock-plugin");
                    }
                }
        ));
    }
}