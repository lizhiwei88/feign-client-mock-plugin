package com.github.lizhiwei88.feign.mock.plugin.ui.json;

import com.github.lizhiwei88.feign.mock.plugin.generator.MockDataGenerator;
import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.intellij.icons.AllIcons;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * JSON编辑面板
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/7
 */
public class JsonEditorPanel extends JBPanel<JsonEditorPanel> {

    private final LanguageTextField jsonEditor;
    private transient FeignMethodNode currentNode;

    public JsonEditorPanel(Project project) {
        super(new CardLayout());
        JBLabel emptyLabel = new JBLabel("");
        // 设置label内容剧中
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JBLabel invalidLabel = new JBLabel("Source code changed, please refresh.", SwingConstants.CENTER);
        invalidLabel.setForeground(JBColor.GRAY);

        this.jsonEditor = createJsonEditor(project);

        ActionToolbar toolbar = createToolbar();

        JBPanel<?> editorPanel = new JBPanel<>(new BorderLayout());
        editorPanel.add(toolbar.getComponent(), BorderLayout.EAST);
        editorPanel.add(jsonEditor, BorderLayout.CENTER);

        add(emptyLabel, "empty");
        add(invalidLabel, "invalid");
        add(editorPanel, "json");
        showEmptyPanel();
    }

    private LanguageTextField createJsonEditor(Project project) {
        LanguageTextField editor = new LanguageTextField(JsonLanguage.INSTANCE, project, "", false) {
            @Override
            protected @NotNull EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                editor.setVerticalScrollbarVisible(true);
                editor.setHorizontalScrollbarVisible(true);
                editor.getSettings().setLineNumbersShown(true);
                editor.getSettings().setFoldingOutlineShown(true);
                editor.getSettings().setIndentGuidesShown(true);
                editor.getSettings().setCaretRowShown(true);
                return editor;
            }
        };
        editor.setPlaceholder("Enter Mock JSON here...");
        return editor;
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Reformat", "Reformat JSON data", AllIcons.Diff.MagicResolveToolbar) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Project project = jsonEditor.getProject();
                Document document = jsonEditor.getDocument();
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if (psiFile != null) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        CodeStyleManager.getInstance(project).reformat(psiFile);
                    });
                }
            }
        });

        group.add(new AnAction("Auto Generate", "Generate mock data based on return type", AllIcons.Actions.Lightning) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (currentNode != null) {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        String json = ReadAction.compute(() -> {
                            PsiMethod method = currentNode.getPsiMethod();
                            if (method == null || !method.isValid()) return null;
                            PsiType returnType = method.getReturnType();
                            if (returnType == null) return null;
                            return MockDataGenerator.generateJson(returnType);
                        });

                        if (json != null) {
                            ApplicationManager.getApplication().invokeLater(() -> jsonEditor.setText(json));
                        }
                    });
                }
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("FeignMockJsonPanel", group, false);
        toolbar.setTargetComponent(jsonEditor);
        // Optimize toolbar appearance: don't reserve space for popup icons if not needed
        toolbar.setReservePlaceAutoPopupIcon(false);
        return toolbar;
    }

    public LanguageTextField getJsonEditor() {
        return jsonEditor;
    }

    public void showEmptyPanel() {
        CardLayout layout = (CardLayout) getLayout();
        layout.show(this, "empty");
    }

    public void showInvalidPanel() {
        CardLayout layout = (CardLayout) getLayout();
        layout.show(this, "invalid");
    }

    public void showJsonEditor() {
        CardLayout layout = (CardLayout) getLayout();
        layout.show(this, "json");
    }

    public void setJsonEditorContent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode != null && selectedNode.getUserObject() instanceof FeignMethodNode nodeData) {
            if (nodeData.getPsiMethod() == null) {
                this.currentNode = nodeData;
                showInvalidPanel();
                return;
            }
            this.currentNode = nodeData;
            showJsonEditor();
            jsonEditor.setText(nodeData.getMockData());
        } else {
            this.currentNode = null;
            showEmptyPanel();
            jsonEditor.setText(null);

        }
    }
}
