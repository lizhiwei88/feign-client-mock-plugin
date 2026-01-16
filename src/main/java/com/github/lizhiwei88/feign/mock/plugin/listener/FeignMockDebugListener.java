package com.github.lizhiwei88.feign.mock.plugin.listener;

import com.github.lizhiwei88.feign.mock.plugin.network.AgentRequestManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManagerListener;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * FeignMockDebugListener
 *
 * @author lizhiwei
 * @version 1.0
 * @since 2026/01/15
 */
public class FeignMockDebugListener implements XDebuggerManagerListener {

    private static final Logger log = Logger.getInstance(FeignMockDebugListener.class);

    // Keep track of observed sessions to avoid duplicate listeners
    private static final Set<XDebugSession> attachedSessions = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    // processStarted and processStopped are removed as they might not be in the interface in newer SDKs
    // or method signature mismatch issues. We use ProjectRunListener to attach initial listeners.

    @Override
    public void currentSessionChanged(XDebugSession previousSession, XDebugSession currentSession) {
        if (currentSession != null) {
            attach(currentSession);
        }
    }

    public static void attach(XDebugSession session) {
        if (session == null) return;

        if (attachedSessions.contains(session)) {
            return;
        }
        attachedSessions.add(session);

        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                // Just log or update status if needed
            }

            @Override
            public void sessionResumed() {
                // This is the key moment: session is back to running state
                Project project = session.getProject();
                log.info("Debug session resumed, checking for pending Feign Mock updates...");
                AgentRequestManager.processPendingRequests(project);
            }
        });
    }
}
