package com.github.lizhiwei88.feign.mock.plugin.settings;

import com.github.lizhiwei88.feign.mock.plugin.patcher.FeignAgentPatcher;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * FeignMockSetting文件加载类
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/6
 */
public class FeignMockSettingFileReload {

    private static final Logger log = Logger.getInstance(FeignAgentPatcher.class);

    private FeignMockSettingFileReload() {
    }

    public static void parseExternalXml(Project project) {
        try (InputStream in = new FileInputStream(project.getBasePath() + "/.idea/feign_mock_settings.xml")) {
            Element root = JDOMUtil.load(in);
            Element component = root.getChild("component");
            if (component == null) return;
            FeignMockSettings.State state = new FeignMockSettings.State();
            for (Element option : component.getChildren("option")) {
                String name = option.getAttributeValue("name");
                String value = option.getAttributeValue("value");
                if ("lastKnownPort".equals(name)) {
                    state.lastKnownPort = Integer.parseInt(value);
                }
            }
            FeignMockSettings service = FeignMockSettings.getInstance(project);
            service.loadState(state);
            log.info("FeignMockSettings reloaded: port=" + state.lastKnownPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}