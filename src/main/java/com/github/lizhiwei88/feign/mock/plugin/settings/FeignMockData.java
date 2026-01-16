package com.github.lizhiwei88.feign.mock.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * FeignMockData 持久化数据
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/31
 */
@Service(Service.Level.PROJECT)
@State(name = "FeignMockData", storages = @Storage("feign_mock_data.xml"))
public final class FeignMockData implements PersistentStateComponent<FeignMockData> {

    // 要持久化的 Map 数据
    public Map<String, String> mapData = new HashMap<>();

    @Override
    public @NotNull FeignMockData getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FeignMockData feignMockData) {
        XmlSerializerUtil.copyBean(feignMockData, this);
    }

    // 获取单例服务
    public static FeignMockData getInstance(@NotNull Project project) {
        return project.getService(FeignMockData.class);
    }

    // 实用方法：设置键值
    public void put(String key, String value) {
        mapData.put(key, value);
    }

    // 实用方法：获取值
    public String get(String key) {
        return mapData.get(key);
    }

    // 实用方法：删除键
    public void remove(String key) {
        mapData.remove(key);
    }

    // 获取整个 Map 副本
    public Map<String, String> getAll() {
        return new HashMap<>(mapData);
    }
}
