package com.github.lizhiwei88.feign.mock.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FeignMockSettings 持久化设置
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2025/12/25
 */
@Service(Service.Level.PROJECT)
@State(name = "FeignMockSettings", storages = @Storage("feign_mock_settings.xml"))
public final class FeignMockSettings implements PersistentStateComponent<FeignMockSettings.State> {

    public static class State {
        public int lastKnownPort = -1;
    }

    private State myState = new State();

    @Override
    public @Nullable State getState() { return myState; }

    @Override
    public void loadState(@NotNull State state) { this.myState = state; }

    public static FeignMockSettings getInstance(Project project) {
        return project.getService(FeignMockSettings.class);
    }
}