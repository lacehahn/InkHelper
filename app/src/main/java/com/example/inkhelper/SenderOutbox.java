package com.example.inkhelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SenderOutbox {
    private static final int MAX_ITEMS = 30;
    private final List<SenderOutboxItem> items = new ArrayList<>();
    private long nextId = 1L;

    public synchronized long record(TransferNotification notification, TransferStatus status, String stage) {
        long id = nextId++;
        SenderOutboxItem item = new SenderOutboxItem(
                id,
                notification == null ? "" : notification.sourceApplication,
                notification == null ? "" : formatTimestamp(notification.capturedAtEpochMillis),
                notification == null ? "" : notification.title,
                notification == null ? "" : notification.text,
                status,
                stage);
        items.add(0, item);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        return id;
    }

    public synchronized void update(long id, TransferStatus status, String stage) {
        for (int index = 0; index < items.size(); index++) {
            SenderOutboxItem item = items.get(index);
            if (item.id == id) {
                items.set(index, item.withStatus(status, stage));
                return;
            }
        }
    }

    public synchronized List<SenderOutboxItem> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized boolean remove(long id) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).id == id) {
                items.remove(index);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean clear() {
        if (items.isEmpty()) {
            return false;
        }
        items.clear();
        return true;
    }

    private static String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "";
        }
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(epochMillis));
    }
}
