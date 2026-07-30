package com.example.inkhelper;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFERENCES = "runtime_role";
    private static final String ROLE_KEY = "selected_role";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private LinearLayout content;
    private EditText receiverAddress;
    private EditText receiverCode;
    private final InkRuntime runtime = InkRuntime.get();
    private final Runnable inboxChangedListener = () -> runOnUiThread(() -> {
        InkLog.d("event=ui_inbox_changed_render_requested activeRole="
                + (runtime.activeRole() == null ? "null" : runtime.activeRole().name()));
        if (runtime.activeRole() == RuntimeRole.RECEIVER) {
            render(RuntimeRole.RECEIVER);
        }
    });
    private final Runnable senderOutboxChangedListener = () -> runOnUiThread(() -> {
        InkLog.d("event=ui_sender_outbox_changed_render_requested activeRole="
                + (runtime.activeRole() == null ? "null" : runtime.activeRole().name()));
        if (runtime.activeRole() == RuntimeRole.SENDER) {
            render(RuntimeRole.SENDER);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        runtime.initialize(getApplicationContext());
        requestNotificationPermissionIfNeeded();
        RuntimeRole restoredRole = readRole();
        runtime.restoreRole(restoredRole);
        updateReceiverBackgroundService(restoredRole);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(R.color.ink_surface));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int padding = dimen(R.dimen.ink_screen_padding);
        page.setPadding(padding, padding, padding, padding);
        scrollView.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });

        content = page;
        setContentView(scrollView);
        render(restoredRole);
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime.addInboxChangedListener(inboxChangedListener);
        runtime.addSenderOutboxChangedListener(senderOutboxChangedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RuntimeRole role = readRole();
        maybeRequestSenderListenerRebind(role);
        render(role);
    }

    @Override
    protected void onStop() {
        runtime.removeInboxChangedListener(inboxChangedListener);
        runtime.removeSenderOutboxChangedListener(senderOutboxChangedListener);
        super.onStop();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void selectRole(RuntimeRole role) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putString(ROLE_KEY, role.name()).apply();
        runtime.selectRole(role);
        updateReceiverBackgroundService(role);
        render(role);
    }

    private RuntimeRole readRole() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        return RuntimeRole.fromStoredValue(preferences.getString(ROLE_KEY, null));
    }

    private void updateReceiverBackgroundService(RuntimeRole role) {
        if (role == RuntimeRole.RECEIVER) {
            ReceiverBackgroundService.start(this);
        } else {
            ReceiverBackgroundService.stop(this);
        }
    }

    private void render(RuntimeRole role) {
        content.removeAllViews();
        addHeader(role);
        addRoleSection(role);
        if (role == RuntimeRole.SENDER) {
            showSender();
        } else if (role == RuntimeRole.RECEIVER) {
            showReceiver();
        } else {
            LinearLayout section = addSection(getString(R.string.status_ready), getString(R.string.role_not_selected));
            section.addView(bodyText(getString(R.string.app_subtitle)));
        }
    }

    private void addHeader(RuntimeRole role) {
        LinearLayout row = horizontalRow();

        TextView appName = new TextView(this);
        appName.setText(R.string.app_name);
        appName.setTextColor(color(R.color.black));
        appName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        appName.setLetterSpacing(0);
        row.addView(appName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialButton settings = actionButton(getString(R.string.settings_label), false, view -> showSettingsDialog(role));
        row.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(row);

        TextView subtitle = new TextView(this);
        subtitle.setText(role == RuntimeRole.SENDER ? R.string.sender_active
                : role == RuntimeRole.RECEIVER ? R.string.receiver_active : R.string.app_subtitle);
        subtitle.setTextColor(color(R.color.ink_text_secondary));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        content.addView(subtitle);
    }

    private void addRoleSection(RuntimeRole role) {
        LinearLayout section = addSection(getString(R.string.role_label), null);
        LinearLayout row = horizontalRow();
        row.addView(actionButton(getString(R.string.select_sender), role == RuntimeRole.SENDER, view -> selectRole(RuntimeRole.SENDER)),
                weightedParams());
        row.addView(actionButton(getString(R.string.select_receiver), role == RuntimeRole.RECEIVER, view -> selectRole(RuntimeRole.RECEIVER)),
                weightedParams());
        section.addView(row);
    }

    private void showSender() {
        LinearLayout sessionSection = addSection(getString(R.string.session_label),
                getString(R.string.sender_session_state, sessionStateText(runtime.senderSessionState())));
        LocalSessionDetails details = runtime.senderSessionDetails();
        if (details != null) {
            sessionSection.addView(dataBlock(getString(R.string.sender_session_details, details.addressWithPort(), details.code)));
        }
        LinearLayout actions = horizontalRow();
        actions.addView(actionButton(getString(R.string.start_sender_session), true, view -> {
            runtime.startSenderSession();
            render(RuntimeRole.SENDER);
        }), weightedParams());
        actions.addView(actionButton(getString(R.string.disconnect_session), false, view -> {
            runtime.disconnectActiveSession();
            render(RuntimeRole.SENDER);
        }), weightedParams());
        sessionSection.addView(actions);
        showSenderOutboxSection();
    }

    private void showSenderOutboxSection() {
        LinearLayout outboxSection = addSection(getString(R.string.sender_outbox_label), "");
        List<SenderOutboxItem> outboxItems = runtime.senderOutboxItems();
        if (outboxItems.isEmpty()) {
            outboxSection.addView(emptyState(getString(R.string.sender_outbox_empty)));
        } else {
            outboxSection.addView(actionButton(getString(R.string.clear_messages), false, view -> {
                runtime.clearSenderOutbox();
                render(RuntimeRole.SENDER);
            }), matchParams());
            for (SenderOutboxItem item : outboxItems) {
                outboxSection.addView(senderOutboxItemView(item));
            }
        }
    }

    private void showReceiver() {
        boolean connected = runtime.receiverSessionState() == SessionState.CONNECTED;
        LinearLayout sessionSection = addSection(getString(R.string.session_label),
                getString(R.string.receiver_session_state, sessionStateText(runtime.receiverSessionState())));
        if (runtime.receiverSessionState() == SessionState.UNAVAILABLE) {
            sessionSection.addView(statusLine(getString(R.string.receiver_operational_unavailable)));
        }
        sessionSection.addView(actionButton(getString(R.string.disconnect_session), false, view -> {
            runtime.disconnectActiveSession();
            render(RuntimeRole.RECEIVER);
        }), matchParams());

        LinearLayout pairingSection = addSection(getString(R.string.pairing_label),
                getString(R.string.pairing_scan_status, runtime.lastSessionMessage()));
        MaterialButton scanButton = actionButton(getString(R.string.scan_for_senders), true, view ->
                new Thread(() -> {
                    InkLog.d("event=ui_scan_clicked");
                    runtime.scanForSenders();
                    runOnUiThread(() -> render(RuntimeRole.RECEIVER));
                }, "InkHelperPairingScan").start());
        scanButton.setEnabled(!connected);
        pairingSection.addView(scanButton, matchParams());
        List<PairingCandidate> candidates = runtime.lastPairingCandidates();
        if (candidates.isEmpty()) {
            pairingSection.addView(bodyText(getString(R.string.no_pairing_candidates)));
        } else {
            for (PairingCandidate candidate : candidates) {
                MaterialButton candidateButton = actionButton(
                        getString(R.string.pairing_candidate, candidate.addressWithPort()),
                        false,
                        view -> {
                            InkLog.d("event=ui_pairing_candidate_clicked address=" + candidate.senderAddress
                                    + " port=" + candidate.sessionPort
                                    + " codeLength=" + candidate.sessionCode.length());
                            connectReceiverInBackground(candidate);
                        });
                candidateButton.setEnabled(!connected);
                pairingSection.addView(candidateButton, matchParams());
            }
        }

        showInboxSection();
    }

    private void showInboxSection() {
        LinearLayout inboxSection = addSection(getString(R.string.inbox_label), "");
        List<InboxItem> inboxItems = runtime.inboxItems();
        if (inboxItems.isEmpty()) {
            inboxSection.addView(emptyState(getString(R.string.receiver_inbox_empty)));
        } else {
            inboxSection.addView(actionButton(getString(R.string.clear_messages), false, view -> {
                runtime.clearInbox();
                render(RuntimeRole.RECEIVER);
            }), matchParams());
            for (InboxItem item : inboxItems) {
                inboxSection.addView(inboxItemView(item));
            }
        }
    }

    private void showSettingsDialog(RuntimeRole role) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        scrollView.addView(panel);

        if (role == RuntimeRole.SENDER) {
            addSenderSettings(panel);
        } else if (role == RuntimeRole.RECEIVER) {
            addReceiverSettings(panel);
        } else {
            panel.addView(bodyText(getString(R.string.role_not_selected)));
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_label)
                .setView(scrollView)
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void addSenderSettings(LinearLayout panel) {
        boolean accessAvailable = SenderNotificationListener.canObserve(this);
        SenderListenerSnapshot listenerSnapshot = runtime.senderListenerSnapshot();
        LinearLayout accessSection = settingsSection(getString(R.string.status_ready),
                getString(R.string.notification_listener_diagnostics,
                        accessAvailable ? getString(R.string.notification_access_available) : getString(R.string.notification_access_unavailable),
                        listenerSnapshot.connected
                                ? getString(R.string.notification_listener_connected)
                                : getString(R.string.notification_listener_disconnected),
                        listenerSnapshot.lastEvent,
                        listenerEventTime(listenerSnapshot)));
        if (!accessAvailable) {
            accessSection.addView(actionButton(getString(R.string.enable_notification_access), true,
                    view -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))), matchParams());
        } else {
            if (!listenerSnapshot.connected) {
                accessSection.addView(bodyText(getString(R.string.notification_listener_restricted_settings_hint)));
                accessSection.addView(actionButton(getString(R.string.open_app_details_settings), false,
                        view -> openAppDetailsSettings()), matchParams());
            }
            accessSection.addView(actionButton(getString(R.string.rebind_notification_listener), false, view -> {
                SenderNotificationListener.requestListenerRebind(this);
                render(RuntimeRole.SENDER);
            }), matchParams());
        }
        panel.addView(accessSection, dialogSectionParams());

        LinearLayout filtersSection = settingsSection(getString(R.string.filters_label),
                getString(R.string.filtering_disabled_mvp));
        panel.addView(filtersSection, dialogSectionParams());

        LinearLayout transferSection = settingsSection(getString(R.string.transfer_label),
                getString(R.string.transfer_status, transferStatusText(runtime.lastTransferStatus())));
        transferSection.addView(actionButton(getString(R.string.send_sample_notification), false,
                view -> sendSampleInBackground()), matchParams());
        panel.addView(transferSection, dialogSectionParams());
    }

    private String listenerEventTime(SenderListenerSnapshot snapshot) {
        if (snapshot == null || snapshot.lastEventAtEpochMillis <= 0L) {
            return getString(R.string.notification_listener_event_never);
        }
        return DateFormat.format("HH:mm:ss", snapshot.lastEventAtEpochMillis).toString();
    }

    private void maybeRequestSenderListenerRebind(RuntimeRole role) {
        if (role == RuntimeRole.SENDER
                && SenderNotificationListener.canObserve(this)
                && !runtime.senderListenerSnapshot().connected) {
            SenderNotificationListener.requestListenerRebind(this);
        }
    }

    private void openAppDetailsSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void addReceiverSettings(LinearLayout panel) {
        LinearLayout notificationSection = settingsSection(getString(R.string.receiver_notification_channel_name),
                ReceiverSystemNotifier.hasNotificationPermission(this)
                        ? getString(R.string.notification_access_available)
                        : getString(R.string.notification_access_unavailable));
        notificationSection.addView(actionButton(getString(R.string.enable_system_notifications), true,
                view -> requestNotificationPermissionIfNeeded()), matchParams());
        panel.addView(notificationSection, dialogSectionParams());

        LinearLayout manualSection = settingsSection(getString(R.string.manual_pairing_label), "");
        receiverAddress = input(getString(R.string.sender_address_hint));
        receiverCode = input(getString(R.string.session_code_hint));
        manualSection.addView(receiverAddress, matchParams());
        manualSection.addView(receiverCode, matchParams());
        manualSection.addView(actionButton(getString(R.string.connect_receiver_session), true, view -> {
            InkLog.d("event=ui_manual_connect_clicked address="
                    + receiverAddress.getText().toString()
                    + " codeLength=" + receiverCode.getText().toString().trim().length());
            connectReceiverInBackground(receiverAddress.getText().toString(), receiverCode.getText().toString());
        }), matchParams());
        panel.addView(manualSection, dialogSectionParams());

        LinearLayout inboxToolsSection = settingsSection(getString(R.string.inbox_label), "");
        inboxToolsSection.addView(actionButton(getString(R.string.add_representative_notification), false, view -> {
            runtime.addRepresentativeNotification();
            render(RuntimeRole.RECEIVER);
        }), matchParams());
        panel.addView(inboxToolsSection, dialogSectionParams());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private LinearLayout settingsSection(String title, String summary) {
        return createSection(title, summary);
    }

    private LinearLayout addSection(String title, String summary) {
        LinearLayout section = createSection(title, summary);
        LinearLayout.LayoutParams params = matchParams();
        params.setMargins(0, 0, 0, dimen(R.dimen.ink_section_gap));
        content.addView(section, params);
        return section;
    }

    private LinearLayout createSection(String title, String summary) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(12), dp(14), dp(12));
        section.setBackground(sectionBackground());

        TextView titleView = labelText(title);
        section.addView(titleView);
        if (summary != null && !summary.trim().isEmpty()) {
            TextView summaryView = bodyText(summary);
            summaryView.setPadding(0, dp(2), 0, dp(8));
            section.addView(summaryView);
        }
        return section;
    }

    private void connectReceiverInBackground(String addressWithPort, String code) {
        new Thread(() -> {
            InkLog.d("event=ui_receiver_connect_thread_start address=" + addressWithPort
                    + " codeLength=" + (code == null ? 0 : code.trim().length()));
            runtime.connectReceiver(addressWithPort, code);
            runOnUiThread(() -> render(RuntimeRole.RECEIVER));
        }, "InkHelperReceiverConnect").start();
    }

    private void connectReceiverInBackground(PairingCandidate candidate) {
        new Thread(() -> {
            InkLog.d("event=ui_receiver_pair_thread_start address=" + candidate.senderAddress
                    + " port=" + candidate.sessionPort
                    + " codeLength=" + candidate.sessionCode.length());
            runtime.connectReceiver(candidate);
            runOnUiThread(() -> render(RuntimeRole.RECEIVER));
        }, "InkHelperReceiverPair").start();
    }

    private void sendSampleInBackground() {
        new Thread(() -> {
            InkLog.d("event=ui_send_sample_thread_start");
            runtime.handleObservedEvent(RuntimeRole.SENDER, true, new ObservedNotificationEvent(
                    "示例应用",
                    System.currentTimeMillis(),
                    "示例标题",
                    "示例内容"));
            runOnUiThread(() -> render(RuntimeRole.SENDER));
        }, "InkHelperSampleTransfer").start();
    }

    private View inboxItemView(InboxItem item) {
        LinearLayout itemView = new LinearLayout(this);
        itemView.setOrientation(LinearLayout.VERTICAL);
        itemView.setPadding(dp(12), dp(10), dp(12), dp(10));
        itemView.setBackground(softSectionBackground());

        itemView.addView(messageHeaderView(
                ApplicationDisplayNames.displayName(item.sourceApplication),
                item.capturedTimestamp,
                view -> {
                    runtime.removeInboxItem(item.id);
                    render(RuntimeRole.RECEIVER);
                }));
        if (!item.title.trim().isEmpty()) {
            TextView title = bodyText(item.title);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            itemView.addView(title);
        }
        if (!item.text.trim().isEmpty()) {
            itemView.addView(bodyText(item.text));
        }

        LinearLayout.LayoutParams params = matchParams();
        params.setMargins(0, dimen(R.dimen.ink_item_gap), 0, 0);
        itemView.setLayoutParams(params);
        return itemView;
    }

    private View senderOutboxItemView(SenderOutboxItem item) {
        LinearLayout itemView = new LinearLayout(this);
        itemView.setOrientation(LinearLayout.VERTICAL);
        itemView.setPadding(dp(12), dp(10), dp(12), dp(10));
        itemView.setBackground(softSectionBackground());

        itemView.addView(messageHeaderView(
                ApplicationDisplayNames.displayName(item.sourceApplication),
                item.capturedTimestamp,
                view -> {
                    runtime.removeSenderOutboxItem(item.id);
                    render(RuntimeRole.SENDER);
                }));
        itemView.addView(bodyText(getString(R.string.sender_outbox_status,
                transferStatusText(item.status),
                item.stage)));
        if (!item.title.trim().isEmpty()) {
            TextView title = bodyText(item.title);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            itemView.addView(title);
        }
        if (!item.text.trim().isEmpty()) {
            itemView.addView(bodyText(item.text));
        }

        LinearLayout.LayoutParams params = matchParams();
        params.setMargins(0, dimen(R.dimen.ink_item_gap), 0, 0);
        itemView.setLayoutParams(params);
        return itemView;
    }

    private View messageHeaderView(String sourceApplication, String capturedTimestamp, View.OnClickListener deleteListener) {
        LinearLayout row = horizontalRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView sourceAndTime = labelText(sourceApplication + "  " + capturedTimestamp);
        sourceAndTime.setSingleLine(true);
        sourceAndTime.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(sourceAndTime, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialButton delete = deleteIconButton(deleteListener);
        row.addView(delete);
        return row;
    }

    private MaterialButton deleteIconButton(View.OnClickListener listener) {
        MaterialButton button = actionButton("X", false, listener);
        button.setContentDescription(getString(R.string.delete_message));
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dimen(R.dimen.ink_touch_target));
        button.setMinHeight(dimen(R.dimen.ink_touch_target));
        button.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dimen(R.dimen.ink_touch_target),
                dimen(R.dimen.ink_touch_target));
        params.setMargins(dimen(R.dimen.ink_item_gap), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private MaterialButton actionButton(String text, boolean primary, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dimen(R.dimen.ink_touch_target));
        button.setCornerRadius(dimen(R.dimen.ink_corner_radius));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setLetterSpacing(0);
        button.setOnClickListener(listener);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(dimen(R.dimen.ink_stroke_width));
        button.setStrokeColor(ColorStateList.valueOf(color(R.color.black)));
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? color(R.color.black) : color(R.color.white)));
        button.setTextColor(primary ? color(R.color.white) : color(R.color.black));
        return button;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextColor(color(R.color.black));
        editText.setHintTextColor(color(R.color.ink_text_secondary));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editText.setMinHeight(dimen(R.dimen.ink_touch_target));
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(inputBackground());
        LinearLayout.LayoutParams params = matchParams();
        params.setMargins(0, 0, 0, dimen(R.dimen.ink_item_gap));
        editText.setLayoutParams(params);
        return editText;
    }

    private TextView labelText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color(R.color.black));
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        text.setLetterSpacing(0);
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color(R.color.ink_text_secondary));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setLineSpacing(dp(2), 1.0f);
        text.setLetterSpacing(0);
        return text;
    }

    private TextView statusLine(String value) {
        TextView text = bodyText(value);
        text.setTextColor(color(R.color.black));
        text.setBackground(softSectionBackground());
        text.setPadding(dp(12), dp(8), dp(12), dp(8));
        return text;
    }

    private TextView emptyState(String value) {
        TextView text = bodyText(value);
        text.setGravity(android.view.Gravity.CENTER);
        text.setMinHeight(dp(96));
        text.setBackground(softSectionBackground());
        return text;
    }

    private View dataBlock(String value) {
        TextView text = bodyText(value);
        text.setTextColor(color(R.color.black));
        text.setTypeface(Typeface.MONOSPACE);
        text.setPadding(dp(12), dp(10), dp(12), dp(10));
        text.setBackground(softSectionBackground());
        LinearLayout.LayoutParams params = matchParams();
        params.setMargins(0, 0, 0, dimen(R.dimen.ink_item_gap));
        text.setLayoutParams(params);
        return text;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        return row;
    }

    private String activeRoleText(RuntimeRole role) {
        if (role == RuntimeRole.SENDER) {
            return getString(R.string.sender_active);
        }
        if (role == RuntimeRole.RECEIVER) {
            return getString(R.string.receiver_active);
        }
        return getString(R.string.role_not_selected);
    }

    private String sessionStateText(SessionState state) {
        if (state == SessionState.CONNECTED) {
            return getString(R.string.session_state_connected);
        }
        if (state == SessionState.CONNECTING) {
            return getString(R.string.session_state_connecting);
        }
        if (state == SessionState.UNAVAILABLE) {
            return getString(R.string.session_state_unavailable);
        }
        return getString(R.string.session_state_disconnected);
    }

    private String transferStatusText(TransferStatus status) {
        if (status == TransferStatus.CONFIRMED_RECEIVED) {
            return getString(R.string.transfer_status_confirmed_received);
        }
        if (status == TransferStatus.NOT_SENT) {
            return getString(R.string.transfer_status_not_sent);
        }
        if (status == TransferStatus.REJECTED) {
            return getString(R.string.transfer_status_rejected);
        }
        if (status == TransferStatus.UNCONFIRMED) {
            return getString(R.string.transfer_status_unconfirmed);
        }
        return getString(R.string.transfer_status_not_started);
    }

    private GradientDrawable sectionBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(R.color.white));
        drawable.setStroke(dimen(R.dimen.ink_stroke_width), color(R.color.black));
        drawable.setCornerRadius(dimen(R.dimen.ink_corner_radius));
        return drawable;
    }

    private GradientDrawable softSectionBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(R.color.ink_surface_variant));
        drawable.setStroke(dimen(R.dimen.ink_stroke_width), color(R.color.ink_outline_soft));
        drawable.setCornerRadius(dimen(R.dimen.ink_corner_radius));
        return drawable;
    }

    private GradientDrawable inputBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(R.color.white));
        drawable.setStroke(dimen(R.dimen.ink_stroke_width), color(R.color.ink_outline));
        drawable.setCornerRadius(dimen(R.dimen.ink_corner_radius));
        return drawable;
    }

    private LinearLayout.LayoutParams matchParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dimen(R.dimen.ink_item_gap), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams dialogSectionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dimen(R.dimen.ink_section_gap));
        return params;
    }

    private LinearLayout.LayoutParams weightedParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, dimen(R.dimen.ink_item_gap), dimen(R.dimen.ink_item_gap), 0);
        return params;
    }

    private int color(int resource) {
        return ContextCompat.getColor(this, resource);
    }

    private int dimen(int resource) {
        return getResources().getDimensionPixelSize(resource);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }
}
