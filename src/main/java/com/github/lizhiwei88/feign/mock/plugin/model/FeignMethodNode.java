package com.github.lizhiwei88.feign.mock.plugin.model;

import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockData;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Feign方法节点
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
public class FeignMethodNode {

    private final SmartPsiElementPointer<PsiMethod> psiMethodPointer;
    private final String signature;
    private final String methodName;

    private String mockData;

    public FeignMethodNode(PsiMethod method) {
        this.psiMethodPointer = SmartPointerManager.getInstance(method.getProject()).createSmartPsiElementPointer(method);
        this.methodName = method.getName();
        this.signature = Objects.requireNonNull(method.getContainingClass()).getQualifiedName() + "#" + method.getName() + "(" +
                String.join(",", Arrays.stream(method.getParameterList().getParameters())
                        .map(param -> {
                            try {
                                return param.getType().getCanonicalText();
                            } catch (Exception e) {
                                // 如果无法获取类型规范文本，则使用类型表示文本作为备选
                                return param.getType().getPresentableText();
                            }
                        })
                        .toArray(String[]::new)) + ")";
        FeignMockData feignMockData = FeignMockData.getInstance(method.getProject());
        this.mockData = feignMockData.get(this.signature);
    }

    @Override
    public String toString() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    public String getMockData() {
        return mockData;
    }

    public void setMockData(String mockData) {
        this.mockData = mockData;
    }

    public PsiMethod getPsiMethod() {
        PsiMethod method = psiMethodPointer.getElement();
        if (method != null && method.isValid()) {
            return method;
        }
        return null;
    }
}