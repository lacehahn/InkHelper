package com.example.inkhelper;

import android.content.Context;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InkRuntime {
    private static final InkRuntime INSTANCE = new InkRuntime();
    private final ReceiverInbox inbox = new ReceiverInbox();
    private final SenderOutbox senderOutbox = new SenderOutbox();
    private final ApplicationAllowlist allowlist = new ApplicationAllowlist();
    private final NotificationEligibility eligibility = new NotificationEligibility();
    private final SenderObservationGate observationGate = new SenderObservationGate();
    private final ReceiverInboundGate inboundGate = new ReceiverInboundGate();
    private final CopyOnWriteArrayList<Runnable> inboxChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> senderOutboxChangedListeners = new CopyOnWriteArrayList<>();
    private final LocalSessionServer senderSession = new LocalSessionServer();
    private final LocalSessionClient receiverSession = new LocalSessionClient();
    private final PairingDiscoveryScanner pairingScanner = new PairingDiscoveryScanner();
    private final PairingScanGate pairingScanGate = new PairingScanGate();
    private final RuntimeRoleState roleState = new RuntimeRoleState();
    private TransferStatus lastTransferStatus = TransferStatus.NOT_STARTED;
    private String lastSessionMessage = "";
    private List<PairingCandidate> lastPairingCandidates = java.util.Collections.emptyList();
    private ReceiverNotificationPresenter receiverNotificationPresenter = ReceiverNotificationPresenter.noop();
    private boolean senderListenerConnected;
    private String senderListenerLastEvent = "尚未收到监听器回调";
    private long senderListenerLastEventAtEpochMillis;

    InkRuntime() {
    }

    public static InkRuntime get() {
        return INSTANCE;
    }

    public synchronized void initialize(Context context) {
        if (context != null) {
            receiverNotificationPresenter = new ReceiverSystemNotifier(context.getApplicationContext());
            AndroidDiscoveryPacketAccess.initialize(context.getApplicationContext());
        }
    }

    synchronized void setReceiverNotificationPresenter(ReceiverNotificationPresenter presenter) {
        receiverNotificationPresenter = presenter == null ? ReceiverNotificationPresenter.noop() : presenter;
    }

    public synchronized void restoreRole(RuntimeRole role) {
        roleState.restore(role);
    }

    public synchronized void selectRole(RuntimeRole role) {
        boolean changed = roleState.select(role);
        InkLog.d("event=runtime_select_role requestedRole=" + (role == null ? "null" : role.name())
                + " changed=" + changed);
        if (role == null) {
            return;
        }
        if (changed) {
            senderSession.stop();
            receiverSession.stop();
            lastPairingCandidates = java.util.Collections.emptyList();
            lastTransferStatus = TransferStatus.NOT_STARTED;
            lastSessionMessage = "";
        }
    }

    public synchronized RuntimeRole activeRole() {
        return roleState.activeRole();
    }

    public synchronized List<InboxItem> inboxItems() {
        return inbox.snapshot();
    }

    public synchronized List<SenderOutboxItem> senderOutboxItems() {
        return senderOutbox.snapshot();
    }

    public synchronized boolean removeInboxItem(long id) {
        boolean removed = inbox.remove(id);
        if (removed) {
            notifyInboxChanged();
        }
        return removed;
    }

    public synchronized boolean clearInbox() {
        boolean cleared = inbox.clear();
        if (cleared) {
            notifyInboxChanged();
        }
        return cleared;
    }

    public synchronized boolean removeSenderOutboxItem(long id) {
        boolean removed = senderOutbox.remove(id);
        if (removed) {
            notifySenderOutboxChanged();
        }
        return removed;
    }

    public synchronized boolean clearSenderOutbox() {
        boolean cleared = senderOutbox.clear();
        if (cleared) {
            notifySenderOutboxChanged();
        }
        return cleared;
    }

    public synchronized boolean addRepresentativeNotification() {
        if (roleState.activeRole() != RuntimeRole.RECEIVER) {
            return false;
        }
        boolean accepted = inbox.accept(new TransferNotification(
                "示例应用",
                System.currentTimeMillis(),
                "示例标题",
                "示例内容"));
        if (accepted) {
            notifyInboxChanged();
        }
        return accepted;
    }

    public void addInboxChangedListener(Runnable listener) {
        if (listener != null) {
            inboxChangedListeners.addIfAbsent(listener);
        }
    }

    public void removeInboxChangedListener(Runnable listener) {
        if (listener != null) {
            inboxChangedListeners.remove(listener);
        }
    }

    public void addSenderOutboxChangedListener(Runnable listener) {
        if (listener != null) {
            senderOutboxChangedListeners.addIfAbsent(listener);
        }
    }

    public void removeSenderOutboxChangedListener(Runnable listener) {
        if (listener != null) {
            senderOutboxChangedListeners.remove(listener);
        }
    }

    public synchronized void addAllowedApplication(String sourceApplication) {
        allowlist.add(sourceApplication);
    }

    public synchronized void removeAllowedApplication(String sourceApplication) {
        allowlist.remove(sourceApplication);
    }

    public synchronized List<String> allowlistSnapshot() {
        return allowlist.snapshot();
    }

    public synchronized void recordSenderListenerEvent(String event, boolean connected) {
        senderListenerConnected = connected;
        senderListenerLastEvent = event == null || event.trim().isEmpty() ? "未知监听器事件" : event.trim();
        senderListenerLastEventAtEpochMillis = System.currentTimeMillis();
        InkLog.d("event=runtime_sender_listener_diagnostic connected=" + senderListenerConnected
                + " lastEvent=" + senderListenerLastEvent
                + " at=" + senderListenerLastEventAtEpochMillis);
    }

    public synchronized SenderListenerSnapshot senderListenerSnapshot() {
        return new SenderListenerSnapshot(
                senderListenerConnected,
                senderListenerLastEvent,
                senderListenerLastEventAtEpochMillis);
    }

    public synchronized LocalSessionDetails startSenderSession() {
        if (roleState.activeRole() != RuntimeRole.SENDER) {
            lastSessionMessage = "发送端角色未启用";
            InkLog.d("event=runtime_start_sender_session_rejected activeRole="
                    + (roleState.activeRole() == null ? "null" : roleState.activeRole().name()));
            return null;
        }
        try {
            LocalSessionDetails details = senderSession.start();
            lastSessionMessage = "连接信息已生成";
            InkLog.d("event=runtime_start_sender_session_success address=" + details.address
                    + " port=" + details.port
                    + " codeLength=" + details.code.length());
            return details;
        } catch (IOException exception) {
            lastSessionMessage = "无法启动本地连接";
            InkLog.w("event=runtime_start_sender_session_failed", exception);
            return null;
        }
    }

    public synchronized void disconnectActiveSession() {
        RuntimeRole role = roleState.activeRole();
        InkLog.d("event=runtime_disconnect_active_session role="
                + (role == null ? "null" : role.name()));
        if (role == RuntimeRole.SENDER) {
            senderSession.stop();
            lastSessionMessage = "本地连接已断开";
        } else if (role == RuntimeRole.RECEIVER) {
            receiverSession.stop();
            lastSessionMessage = "本地连接已断开";
        }
    }

    public synchronized SessionState connectReceiver(String addressWithPort, String code) {
        if (roleState.activeRole() != RuntimeRole.RECEIVER) {
            lastSessionMessage = "接收端角色未启用";
            InkLog.d("event=runtime_receiver_connect_rejected_not_receiver activeRole="
                    + (roleState.activeRole() == null ? "null" : roleState.activeRole().name()));
            return SessionState.UNAVAILABLE;
        }
        ParsedAddress parsedAddress = ParsedAddress.parse(addressWithPort);
        if (parsedAddress == null) {
            lastSessionMessage = "发送端地址无效";
            InkLog.d("event=runtime_receiver_connect_invalid_address addressWithPort=" + addressWithPort);
            return SessionState.UNAVAILABLE;
        }
        if (receiverSession.state() == SessionState.CONNECTED) {
            lastSessionMessage = "本地连接已建立";
            InkLog.d("event=runtime_receiver_connect_already_connected host=" + parsedAddress.host
                    + " port=" + parsedAddress.port);
            return SessionState.CONNECTED;
        }
        InkLog.d("event=runtime_receiver_connect_begin host=" + parsedAddress.host
                + " port=" + parsedAddress.port
                + " codeLength=" + (code == null ? 0 : code.trim().length()));
        SessionState state;
        try {
            state = receiverSession.connect(parsedAddress.host, parsedAddress.port, code, this::receiveInbound);
        } catch (RuntimeException exception) {
            state = SessionState.UNAVAILABLE;
            InkLog.w("event=runtime_receiver_connect_exception host=" + parsedAddress.host
                    + " port=" + parsedAddress.port, exception);
        }
        lastSessionMessage = state == SessionState.CONNECTED ? "本地连接已建立" : "本地连接不可用";
        InkLog.d("event=runtime_receiver_connect_result state=" + state.name()
                + " message=" + lastSessionMessage);
        return state;
    }

    public List<PairingCandidate> scanForSenders() {
        if (!pairingScanGate.canScan(activeRole())) {
            synchronized (this) {
                lastPairingCandidates = java.util.Collections.emptyList();
                lastSessionMessage = "接收端角色未启用";
                InkLog.d("event=runtime_pairing_scan_rejected activeRole="
                        + (activeRole() == null ? "null" : activeRole().name()));
                return lastPairingCandidates;
            }
        }
        InkLog.d("event=runtime_pairing_scan_begin");
        List<PairingCandidate> candidates = pairingScanner.scan();
        synchronized (this) {
            lastPairingCandidates = candidates;
            lastSessionMessage = candidates.isEmpty() ? "未发现发送端" : "已发现发送端";
            InkLog.d("event=runtime_pairing_scan_result candidates=" + candidates.size()
                    + " message=" + lastSessionMessage);
            return lastPairingCandidates;
        }
    }

    public synchronized SessionState connectReceiver(PairingCandidate candidate) {
        if (candidate == null || !candidate.isValid()) {
            lastSessionMessage = "发送端候选无效";
            InkLog.d("event=runtime_pairing_candidate_invalid");
            return SessionState.UNAVAILABLE;
        }
        InkLog.d("event=runtime_pairing_candidate_selected address=" + candidate.senderAddress
                + " port=" + candidate.sessionPort
                + " codeLength=" + candidate.sessionCode.length());
        return connectReceiver(candidate.addressWithPort(), candidate.sessionCode);
    }

    public synchronized TransferStatus handleObservedEvent(
            RuntimeRole role,
            boolean notificationAccessAvailable,
            ObservedNotificationEvent event) {
        InkLog.d("event=runtime_observed_event role=" + (role == null ? "null" : role.name())
                + " access=" + notificationAccessAvailable
                + " source=" + (event == null ? "null" : event.sourceApplication));
        ObservedNotificationEvent observed = observationGate.observe(role, notificationAccessAvailable, event);
        if (observed == null) {
            lastTransferStatus = TransferStatus.NOT_SENT;
            if (event != null) {
                senderOutbox.record(toTransferNotification(event), lastTransferStatus, "观察门禁拒绝");
                notifySenderOutboxChanged();
            }
            InkLog.d("event=runtime_observed_event_rejected_by_gate status=" + lastTransferStatus.name());
            return lastTransferStatus;
        }
        long outboxId = senderOutbox.record(toTransferNotification(observed), TransferStatus.NOT_STARTED, "已观察");
        notifySenderOutboxChanged();
        EligibilityResult result = eligibility.evaluate(observed, allowlist);
        if (!result.accepted) {
            lastTransferStatus = TransferStatus.NOT_SENT;
            senderOutbox.update(outboxId, lastTransferStatus, "资格检查拒绝");
            notifySenderOutboxChanged();
            InkLog.d("event=runtime_observed_event_rejected_by_eligibility status=" + lastTransferStatus.name());
            return lastTransferStatus;
        }
        senderOutbox.update(outboxId, TransferStatus.NOT_STARTED, "准备发送");
        notifySenderOutboxChanged();
        lastTransferStatus = transfer(result.notification);
        senderOutbox.update(outboxId, lastTransferStatus, transferStage(lastTransferStatus));
        notifySenderOutboxChanged();
        InkLog.d("event=runtime_observed_event_transfer_result status=" + lastTransferStatus.name());
        return lastTransferStatus;
    }

    public synchronized TransferStatus transfer(TransferNotification notification) {
        if (roleState.activeRole() != RuntimeRole.SENDER || senderSession.state() != SessionState.CONNECTED) {
            lastTransferStatus = TransferStatus.NOT_SENT;
            InkLog.d("event=runtime_transfer_not_sent activeRole="
                    + (roleState.activeRole() == null ? "null" : roleState.activeRole().name())
                    + " senderState=" + senderSession.state().name());
            return lastTransferStatus;
        }
        lastTransferStatus = senderSession.transfer(notification);
        InkLog.d("event=runtime_transfer_result status=" + lastTransferStatus.name());
        return lastTransferStatus;
    }

    public synchronized boolean receiveInbound(String sessionId, TransferNotification notification) {
        boolean acceptedByGate = inboundGate.accepts(
                roleState.activeRole(),
                receiverSession.state(),
                receiverSession.sessionId(),
                sessionId,
                notification);
        InkLog.d("event=runtime_receive_inbound sessionId=" + sessionId
                + " acceptedByGate=" + acceptedByGate
                + " receiverState=" + receiverSession.state().name());
        if (!acceptedByGate) {
            return false;
        }
        boolean accepted = inbox.accept(notification);
        InkLog.d("event=runtime_receive_inbound_result accepted=" + accepted
                + " inboxItems=" + inbox.snapshot().size());
        if (accepted) {
            boolean notified = receiverNotificationPresenter.show(notification);
            InkLog.d("event=runtime_receive_inbound_system_notification notified=" + notified);
            notifyInboxChanged();
        }
        return accepted;
    }

    private void notifyInboxChanged() {
        InkLog.d("event=runtime_inbox_changed items=" + inbox.snapshot().size()
                + " listeners=" + inboxChangedListeners.size());
        for (Runnable listener : inboxChangedListeners) {
            try {
                listener.run();
            } catch (RuntimeException exception) {
                InkLog.w("event=runtime_inbox_listener_failed", exception);
            }
        }
    }

    private void notifySenderOutboxChanged() {
        InkLog.d("event=runtime_sender_outbox_changed items=" + senderOutbox.snapshot().size()
                + " listeners=" + senderOutboxChangedListeners.size());
        for (Runnable listener : senderOutboxChangedListeners) {
            try {
                listener.run();
            } catch (RuntimeException exception) {
                InkLog.w("event=runtime_sender_outbox_listener_failed", exception);
            }
        }
    }

    private static TransferNotification toTransferNotification(ObservedNotificationEvent event) {
        return new TransferNotification(
                event == null ? "" : event.sourceApplication,
                event == null || event.capturedAtEpochMillis == null ? 0L : event.capturedAtEpochMillis,
                event == null ? "" : event.title,
                event == null ? "" : event.text);
    }

    private static String transferStage(TransferStatus status) {
        if (status == TransferStatus.CONFIRMED_RECEIVED) {
            return "接收端已确认";
        }
        if (status == TransferStatus.REJECTED) {
            return "接收端已拒绝";
        }
        if (status == TransferStatus.UNCONFIRMED) {
            return "发送未确认";
        }
        if (status == TransferStatus.NOT_SENT) {
            return "未发送";
        }
        return "尚未开始";
    }

    public synchronized SessionState senderSessionState() {
        return senderSession.state();
    }

    public synchronized SessionState receiverSessionState() {
        return receiverSession.state();
    }

    public synchronized LocalSessionDetails senderSessionDetails() {
        return senderSession.details();
    }

    public synchronized TransferStatus lastTransferStatus() {
        return lastTransferStatus;
    }

    public synchronized String lastSessionMessage() {
        return lastSessionMessage;
    }

    public synchronized List<PairingCandidate> lastPairingCandidates() {
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(lastPairingCandidates));
    }

    static final class ParsedAddress {
        final String host;
        final int port;

        private ParsedAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        static ParsedAddress parse(String addressWithPort) {
            if (addressWithPort == null) {
                return null;
            }
            String value = addressWithPort.trim();
            int separator = value.lastIndexOf(':');
            if (separator <= 0 || separator == value.length() - 1) {
                return null;
            }
            try {
                int port = Integer.parseInt(value.substring(separator + 1));
                if (port <= 0) {
                    return null;
                }
                return new ParsedAddress(value.substring(0, separator), port);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }
}
