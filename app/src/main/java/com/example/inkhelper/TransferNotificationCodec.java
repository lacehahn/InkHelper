package com.example.inkhelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class TransferNotificationCodec {
    private TransferNotificationCodec() {
    }

    public static String encode(TransferNotification notification) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeUTF(notification.sourceApplication);
        output.writeLong(notification.capturedAtEpochMillis);
        output.writeUTF(notification.title);
        output.writeUTF(notification.text);
        output.flush();
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public static TransferNotification decode(String payload) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(payload);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        TransferNotification notification = new TransferNotification(
                input.readUTF(),
                input.readLong(),
                input.readUTF(),
                input.readUTF());
        if (!notification.isValid()) {
            throw new IOException("Malformed transfer notification");
        }
        return notification;
    }
}
