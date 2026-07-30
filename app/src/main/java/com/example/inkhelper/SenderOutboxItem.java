package com.example.inkhelper;

public final class SenderOutboxItem {
    public final long id;
    public final String sourceApplication;
    public final String capturedTimestamp;
    public final String title;
    public final String text;
    public final TransferStatus status;
    public final String stage;

    SenderOutboxItem(
            long id,
            String sourceApplication,
            String capturedTimestamp,
            String title,
            String text,
            TransferStatus status,
            String stage) {
        this.id = id;
        this.sourceApplication = sourceApplication == null ? "" : sourceApplication;
        this.capturedTimestamp = capturedTimestamp == null ? "" : capturedTimestamp;
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
        this.status = status == null ? TransferStatus.NOT_STARTED : status;
        this.stage = stage == null ? "" : stage;
    }

    SenderOutboxItem withStatus(TransferStatus nextStatus, String nextStage) {
        return new SenderOutboxItem(
                id,
                sourceApplication,
                capturedTimestamp,
                title,
                text,
                nextStatus,
                nextStage);
    }
}
