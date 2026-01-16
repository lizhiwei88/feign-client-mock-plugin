package com.github.lizhiwei88.feign.mock.plugin.actions;

import com.github.lizhiwei88.feign.mock.plugin.notifications.NotificationUtil;
import com.github.lizhiwei88.feign.mock.plugin.ui.FeignMockTab;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Jump to Feign Mock Action
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/16
 */
public class JumpToFeignMockAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);

        if (project == null || psiElement == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        PsiMethod method = resolveMethod(psiElement);
        if (method != null && isFeignClientMethod(method)) {
            e.getPresentation().setEnabledAndVisible(true);
            e.getPresentation().setText("Jump to Feign Mock");
        } else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (project == null || psiElement == null) return;

        PsiMethod method = ReadAction.compute(() -> resolveMethod(psiElement));
        if (method == null) return;

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Feign Client Mock");
        if (toolWindow != null) {
            toolWindow.activate(() -> {
                Content content = toolWindow.getContentManager().getContent(0);
                if (content != null) {
                    JComponent component = content.getComponent();
                    if (component instanceof FeignMockTab tab) {
                        boolean selected = tab.selectMethod(method);
                        if (!selected) {
                            tab.refreshTree(() -> {
                                if (!tab.selectMethod(method)) {
                                    NotificationUtil.showError(project, "Feign method not found");
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private PsiMethod resolveMethod(PsiElement element) {
        if (element instanceof PsiMethod method) {
            return method;
        }
        if (element instanceof PsiMethodCallExpression callExpression) {
            return callExpression.resolveMethod();
        }
        // Handle case where cursor is on the method name call
        PsiElement parent = element.getParent();
        if (parent instanceof PsiMethodCallExpression callExpression) {
             return callExpression.resolveMethod();
        }
        // Handle method reference
        if (element instanceof PsiReferenceExpression expression) {
             PsiElement resolved = expression.resolve();
             if (resolved instanceof PsiMethod method) {
                 return method;
             }
        }

        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }

    private boolean isFeignClientMethod(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return false;
        return containingClass.hasAnnotation("org.springframework.cloud.openfeign.FeignClient");
    }
}
