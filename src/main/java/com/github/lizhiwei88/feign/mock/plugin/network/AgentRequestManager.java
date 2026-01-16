package com.github.lizhiwei88.feign.mock.plugin.network;

import com.github.lizhiwei88.feign.mock.plugin.settings.FeignMockData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentRequestManager handles network requests with debug session awareness.
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/15
 */
public class AgentRequestManager {
    private static final Logger log = Logger.getInstance(AgentRequestManager.class);

    // Prevent instantiation
    private AgentRequestManager() {}

    public static final String STATUS_SUSPENDED = "Suspended";

    // Store signatures that need to be updated after resume
    private static final Set<String> pendingSignatures = ConcurrentHashMap.newKeySet();

    public static String sendUpdate(Project project, String signature, String json) {
        if (isExecutionSuspended(project)) {
            log.warn("Target application is suspended. Update request deferred for: " + signature);
            pendingSignatures.add(signature);
            return STATUS_SUSPENDED;
        }
        return AgentHttpClient.sendUpdate(project, signature, json);
    }

    public static String sendClear(Project project, String signature) {
        if (isExecutionSuspended(project)) {
            log.warn("Target application is suspended. Clear request deferred for: " + signature);
            pendingSignatures.add(signature);
            return STATUS_SUSPENDED;
        }
        return AgentHttpClient.sendClear(project, signature);
    }

    public static String sendPing(Project project) {
        if (isExecutionSuspended(project)) {
            return STATUS_SUSPENDED;
        }
        return AgentHttpClient.sendPing(project);
    }

    private static boolean isExecutionSuspended(Project project) {
        if (project == null || project.isDisposed()) return false;

        try {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
            if (debuggerManager == null) return false;

            XDebugSession[] sessions = debuggerManager.getDebugSessions();
            for (XDebugSession session : sessions) {
                if (session.isPaused()) {
                    return true;
                }
            }
        } catch (NoClassDefFoundError e) {
            // In case XDebugger is not available (e.g. some minimalist IDE distributions)
            log.warn("XDebuggerManager not found, skipping breakpoint detection.");
        }
        return false;
    }

    /**
     * Replays pending requests when execution resumes.
     * Guaranteed to run on background thread.
     */
    public static void processPendingRequests(Project project) {
        if (pendingSignatures.isEmpty()) return;

        log.info("Processing " + pendingSignatures.size() + " pending requests...");

        // Execute in background to avoid blocking critical threads
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            FeignMockData feignMockData = FeignMockData.getInstance(project);
            if (feignMockData == null) return;

            // Iterate over a copy or iterator to avoid ConcurrentModification if new items added (unlikely during resume processing but safe)
            // However, we can just iterate the set. Since we are in a resumed state, we assume requests can go through.
            // But if it suspends again quickly, we might add more.

            Set<String> processed = ConcurrentHashMap.newKeySet();

            for (String signature : pendingSignatures) {
                String currentJson = feignMockData.get(signature);
                String result;

                if (currentJson != null) {
                    // It exists in data, so it was an update (or create)
                    log.info("Replaying Update for: " + signature);
                    result = AgentHttpClient.sendUpdate(project, signature, currentJson);
                } else {
                    // It does not exist, so it was a deleted
                    log.info("Replaying Clear for: " + signature);
                    result = AgentHttpClient.sendClear(project, signature);
                }

                // If successful or effectively processed, remove from pending
                // We don't want to infinite loop if it fails, so we remove it anyway?
                // Or maybe keep it if failed? For now, remove to avoid stuck queue.
                processed.add(signature);
            }

            pendingSignatures.removeAll(processed);
            log.info("Pending requests processed.");
        });
    }
}
