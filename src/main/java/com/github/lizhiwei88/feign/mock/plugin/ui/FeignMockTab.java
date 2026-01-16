package com.github.lizhiwei88.feign.mock.plugin.ui;

import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.github.lizhiwei88.feign.mock.plugin.ui.json.FeignMockJsonPanel;
import com.github.lizhiwei88.feign.mock.plugin.ui.tree.FeignClientDataLoader;
import com.github.lizhiwei88.feign.mock.plugin.ui.tree.FeignClientTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * FeignMockTab 主面板
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/26
 */
public class FeignMockTab extends JBPanel<FeignMockTab> implements Disposable {

    private final transient Project project;

    private final FeignClientTree feignTree;

    private final FeignMockJsonPanel feignMockJsonPanel;

    public FeignMockTab(Project project) {
        this.project = project;
        // 构建UI
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(0, -1));

        // 1. 初始化 Tree
        feignTree = new FeignClientTree();

        // 2. 初始化 JSON Panel
        feignMockJsonPanel = new FeignMockJsonPanel(project, feignTree);

        // 3. 初始化 Splitter
        JBSplitter splitter = new JBSplitter(true, 0.4f);
        splitter.setFirstComponent(new JBScrollPane(feignTree));
        splitter.setSecondComponent(feignMockJsonPanel);

        // 加入panel
        add(splitter, BorderLayout.CENTER);

        // 4. 绑定事件
        feignTree.setOnMethodSelected(node -> feignMockJsonPanel.getJsonEditorPanel().setJsonEditorContent(node));

        // 5. 初始加载
        refreshTree();
    }

    public void refreshTree() {
        refreshTree(null);
    }

    public void refreshTree(Runnable onComplete) {
        // 显示加载态
        feignTree.setPaintBusy(true);

        // 使用非阻塞式读取 action
        ReadAction.nonBlocking(() -> FeignClientDataLoader.load(project))
                .inSmartMode(project) // 确保索引已准备好
                .expireWith(this) // 如果当前 UI 组件销毁，自动取消任务
                .finishOnUiThread(ModalityState.defaultModalityState(), clientDatas -> {
                    // --- 核心：这部分回到 UI 线程执行，负责渲染 ---
                    feignTree.setPaintBusy(false);
                    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Feign Clients");
                    for (FeignClientDataLoader.ClientData data : clientDatas) {
                        DefaultMutableTreeNode clientNode = new DefaultMutableTreeNode(data.name);
                        for (FeignMethodNode methodNode : data.methods) {
                            clientNode.add(new DefaultMutableTreeNode(methodNode));
                        }
                        root.add(clientNode);
                    }
                    feignTree.setModel(new DefaultTreeModel(root));
                    feignTree.getEmptyText().setText("No feign clients found");

                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    public boolean selectMethod(com.intellij.psi.PsiMethod method) {
        return feignTree.selectMethod(method);
    }

    @Override
    public void dispose() {
        // 释放资源
    }
}
