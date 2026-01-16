package com.github.lizhiwei88.feign.mock.plugin.ui.tree;

import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Feign客户端树形组件
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/16
 */
public class FeignClientTree extends Tree {

    public FeignClientTree() {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("Loading...")));
        setCellRenderer(new NoSelectionTreeCellRenderer());
        initListeners();
    }

    private void initListeners() {
        // 右键菜单
        addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                TreePath path = getPathForLocation(x, y);
                if (path == null) return;
                setSelectionPath(path);

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof FeignMethodNode methodNode) {
                    showContextMenu(comp, x, y, methodNode);
                }
            }
        });
    }

    private void showContextMenu(Component comp, int x, int y, FeignMethodNode methodNode) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Jump to Source", "Navigate to source code", AllIcons.Actions.EditSource) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                PsiMethod method = methodNode.getPsiMethod();
                // 防御性检查
                if (method != null && method.isValid() && method.canNavigate()) {
                    method.navigate(true);
                }
            }
        });

        ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu("FeignClientTreePopup", group);
        popupMenu.getComponent().show(comp, x, y);
    }

    public void setOnMethodSelected(Consumer<DefaultMutableTreeNode> listener) {
        addTreeSelectionListener(e -> {
            Object lastPathComponent = getLastSelectedPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode selectedNode) {
                listener.accept(selectedNode);
            } else {
                listener.accept(null);
            }
        });
    }

    public boolean selectMethod(PsiMethod targetMethod) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

        // 遍历树节点寻找目标方法
        int clientCount = root.getChildCount();
        for (int i = 0; i < clientCount; i++) {
            DefaultMutableTreeNode clientNode = (DefaultMutableTreeNode) root.getChildAt(i);
            int methodCount = clientNode.getChildCount();
            for (int j = 0; j < methodCount; j++) {
                DefaultMutableTreeNode methodNode = (DefaultMutableTreeNode) clientNode.getChildAt(j);
                if (methodNode.getUserObject() instanceof FeignMethodNode node) {
                    PsiMethod psiMethod = node.getPsiMethod();
                    // 使用 Manager 判断元素是否相等
                    if (psiMethod != null && psiMethod.getManager().areElementsEquivalent(psiMethod, targetMethod)) {
                        TreePath path = new TreePath(methodNode.getPath());
                        setSelectionPath(path);
                        scrollPathToVisible(path);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
