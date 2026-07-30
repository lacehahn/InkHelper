package com.example.inkhelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ReceiverInbox {
    private final List<InboxItem> items = new ArrayList<>();
    private long nextId = 1L;

    public synchronized boolean accept(TransferNotification notification) {
        if (notification == null || !notification.isValid()) {
            return false;
        }
        items.add(new InboxItem(
                nextId++,
                notification.sourceApplication,
                formatTimestamp(notification.capturedAtEpochMillis),
                notification.title,
                notification.text));
        return true;
    }

    public synchronized List<InboxItem> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
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
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(epochMillis));
    }
}
