package com.github.lizhiwei88.feign.mock.plugin.ui.tree;

import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * 无选中状态的树节点渲染器
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/6
 */
public class NoSelectionTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Icon methodSuccessIcon = AllIcons.Status.Success;
    private static final Icon methodJsonIcon = AllIcons.FileTypes.Json;

    public NoSelectionTreeCellRenderer() {
        // 将渲染器内部维护的选中/非选中背景都设为透明（或与 tree 背景一致）
        Color transparent = new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));
        setBackgroundSelectionColor(transparent);
        setBackgroundNonSelectionColor(transparent);
        setBorderSelectionColor(null);
        // 如果你希望选中时文字颜色也不变，可以把 text selection color 设为非选中颜色
        setTextSelectionColor(getTextNonSelectionColor());
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        // 关键：让 JLabel 不绘制自己的背景
        label.setOpaque(false);
        // 选择不需要单独的选中效果：确保前景色不随 sel 改变（上面已设置 text selection color）
        // 根据节点类型设置图标
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (userObject instanceof FeignMethodNode feignMethodNode) {
            if (feignMethodNode.getMockData() != null) {
                label.setIcon(methodSuccessIcon);
            } else {
                label.setIcon(methodJsonIcon);
            }
        }
        return label;
    }
}