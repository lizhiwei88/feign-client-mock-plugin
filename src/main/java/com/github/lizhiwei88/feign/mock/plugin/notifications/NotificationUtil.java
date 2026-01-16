package com.github.lizhiwei88.feign.mock.plugin.notifications;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * NotificationUtil
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/1/6
 */
public class NotificationUtil {

    private NotificationUtil() {
    }

    public static void showInfo(Project project, String message) {
        NotificationGroupManager.getInstance().getNotificationGroup("FeignClientPluginNotificationGroup").createNotification(message, NotificationType.INFORMATION).notify(project);
    }

    public static void showError(Project project, String message) {
        NotificationGroupManager.getInstance().getNotificationGroup("FeignClientPluginNotificationGroup").createNotification(message, NotificationType.ERROR).notify(project);
    }
}
