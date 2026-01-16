package com.github.lizhiwei88.feign.mock.plugin.scanner;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * FeignClient接口扫描器
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
public class FeignClientScanner {

    public static final String FEIGN_CLIENT_ANNOTATION = "org.springframework.cloud.openfeign.FeignClient";

    private FeignClientScanner() {

    }

    /**
     * 扫描项目中所有的 FeignClient 接口
     */
    public static List<PsiClass> findAllFeignClients(Project project) {
        List<PsiClass> result = new ArrayList<>();

        // 1. 获取 FeignClient 注解的 PsiClass
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass feignAnnotation = facade.findClass(FEIGN_CLIENT_ANNOTATION, GlobalSearchScope.allScope(project));

        if (feignAnnotation != null) {
            // 2. 搜索所有使用了该注解的接口
            Query<PsiClass> query = AnnotatedElementsSearch.searchPsiClasses(feignAnnotation, GlobalSearchScope.projectScope(project));
            // 3. 对搜索结果进行排序
            List<PsiClass> allResults = query.findAll().stream()
                    .sorted((o1, o2) -> {
                        // 首先比较类名，如果类名相同则比较全限定名
                        int nameComparison = o1.getName().compareTo(Objects.requireNonNull(o2.getName()));
                        if (nameComparison != 0) {
                            return nameComparison;
                        }
                        String qualifiedName1 = o1.getQualifiedName();
                        String qualifiedName2 = o2.getQualifiedName();
                        return qualifiedName1 != null && qualifiedName2 != null ?
                                qualifiedName1.compareTo(qualifiedName2) :
                                0;
                    })
                    .toList();
            result.addAll(allResults);
        }
        return result;
    }

    /**
     * 将 PsiMethod 转换为与 Agent 匹配的 Key (ClassName#MethodName)
     */
    public static String getMethodSignature(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return "";

        StringBuilder signature = new StringBuilder();
        signature.append(containingClass.getQualifiedName())
                .append("#")
                .append(method.getName())
                .append("(");

        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            // 获取参数的全限定类型，例如 java.lang.String 或 com.example.UserDTO
            PsiType type = parameters[i].getType();

            // getCanonicalText() 能准确拿到包名全路径
            signature.append(TypeConversionUtil.erasure(type).getCanonicalText());

            if (i < parameters.length - 1) {
                signature.append(",");
            }
        }
        signature.append(")");

        return signature.toString();
    }
}