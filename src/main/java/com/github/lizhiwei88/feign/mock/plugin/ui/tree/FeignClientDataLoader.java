package com.github.lizhiwei88.feign.mock.plugin.ui.tree;

import com.github.lizhiwei88.feign.mock.plugin.model.FeignMethodNode;
import com.github.lizhiwei88.feign.mock.plugin.scanner.FeignClientScanner;
import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockData;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Feign客户端数据加载器
 * 负责扫描项目并构建树形数据模型
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/16
 */
public class FeignClientDataLoader {

    /**
     * 加载 Feign 客户端数据
     *
     * @param project 当前项目
     * @return 客户端数据列表
     */
    public static List<ClientData> load(Project project) {
        List<ClientData> clientDataList = new ArrayList<>();
        if (project.isDisposed()) return clientDataList;

        List<PsiClass> clients = FeignClientScanner.findAllFeignClients(project);
        // 提前获取 Mock 数据中心实例
        FeignMockData mockDataStorage = FeignMockData.getInstance(project);

        for (PsiClass client : clients) {
            // 增加 isValid 检查，防止 PSI 失效
            if (!client.isValid()) continue;

            List<FeignMethodNode> methodNodes = Arrays.stream(client.getMethods())
                    .filter(PsiMethod::isValid) // 过滤无效方法
                    .map(method -> {
                        FeignMethodNode node = new FeignMethodNode(method);
                        // 确保数据是最新的
                        node.setMockData(mockDataStorage.get(node.getSignature()));
                        return node;
                    })
                    .toList();

            if (!methodNodes.isEmpty()) {
                clientDataList.add(new ClientData(client.getName(), methodNodes));
            }
        }
        return clientDataList;
    }

    /**
     * 数据传输对象：包含客户端名称和方法列表
     */
    public static class ClientData {
        public final String name;
        public final List<FeignMethodNode> methods;

        public ClientData(String name, List<FeignMethodNode> methods) {
            this.name = name;
            this.methods = methods;
        }
    }
}

