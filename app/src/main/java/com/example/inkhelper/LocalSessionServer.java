package com.example.inkhelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

public final class LocalSessionServer {
    private static final int SOCKET_TIMEOUT_MILLIS = 10000;
    private final SecureRandom random = new SecureRandom();
    private ServerSocket serverSocket;
    private Socket connectedSocket;
    private BufferedReader connectedReader;
    private PrintWriter connectedWriter;
    private final PairingDiscoveryResponder discoveryResponder = new PairingDiscoveryResponder();
    private Thread acceptThread;
    private volatile SessionState state = SessionState.DISCONNECTED;
    private volatile boolean stopped = true;
    private LocalSessionDetails details;
    private String sessionId;

    public synchronized LocalSessionDetails start() throws IOException {
        stop();
        stopped = false;
        serverSocket = new ServerSocket(0);
        details = new LocalSessionDetails(LocalAddress.discover(), serverSocket.getLocalPort(), nextCode());
        state = SessionState.CONNECTING;
        InkLog.d("event=sender_session_start address=" + details.address
                + " port=" + details.port
                + " codeLength=" + details.code.length());
        acceptThread = new Thread(this::acceptLoop, "InkHelperSenderSession");
        acceptThread.setDaemon(true);
        acceptThread.start();
        startDiscovery();
        return details;
    }

    public synchronized TransferStatus transfer(TransferNotification notification) {
        if (state != SessionState.CONNECTED || connectedSocket == null || notification == null || !notification.isValid()) {
            InkLog.d("event=sender_transfer_not_sent state=" + state.name()
                    + " socketPresent=" + (connectedSocket != null)
                    + " notificationValid=" + (notification != null && notification.isValid()));
            return TransferStatus.NOT_SENT;
        }
        String transferId = UUID.randomUUID().toString();
        InkLog.d("event=sender_transfer_start transferId=" + transferId
                + " source=" + notification.sourceApplication);
        try {
            connectedWriter.println("NOTIFICATION " + transferId + " " + TransferNotificationCodec.encode(notification));
            if (connectedWriter.checkError()) {
                InkLog.d("event=sender_transfer_write_failed transferId=" + transferId);
                markUnavailable();
                return TransferStatus.UNCONFIRMED;
            }
            InkLog.d("event=sender_transfer_sent transferId=" + transferId);
            String response = connectedReader.readLine();
            if (("ACK " + transferId).equals(response)) {
                InkLog.d("event=sender_transfer_confirmed transferId=" + transferId);
                return TransferStatus.CONFIRMED_RECEIVED;
            }
            if (("REJECT " + transferId).equals(response)) {
                InkLog.d("event=sender_transfer_rejected transferId=" + transferId);
                return TransferStatus.REJECTED;
            }
            InkLog.d("event=sender_transfer_unexpected_response transferId=" + transferId
                    + " response=" + (response == null ? "null" : response));
            markUnavailable();
            return TransferStatus.UNCONFIRMED;
        } catch (IOException | RuntimeException exception) {
            InkLog.w("event=sender_transfer_failed transferId=" + transferId, exception);
            markUnavailable();
            return TransferStatus.UNCONFIRMED;
        }
    }

    public synchronized void stop() {
        InkLog.d("event=sender_session_stop previousState=" + state.name());
        stopped = true;
        closeConnectedSocket();
        closeServerSocket();
        discoveryResponder.stop();
        AndroidDiscoveryPacketAccess.release("sender_responder");
        state = SessionState.DISCONNECTED;
        details = null;
        sessionId = null;
    }

    public synchronized void markUnavailable() {
        InkLog.d("event=sender_session_mark_unavailable previousState=" + state.name());
        closeConnectedSocket();
        closeServerSocket();
        discoveryResponder.stop();
        AndroidDiscoveryPacketAccess.release("sender_responder");
        state = SessionState.UNAVAILABLE;
    }

    public SessionState state() {
        return state;
    }

    public LocalSessionDetails details() {
        return details;
    }

    public String sessionId() {
        return sessionId;
    }

    private void acceptLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
                InkLog.d("event=sender_session_candidate_accepted remote=" + socket.getRemoteSocketAddress());
                handleCandidate(socket);
            } catch (IOException exception) {
                if (!stopped && state != SessionState.DISCONNECTED) {
                    InkLog.w("event=sender_session_accept_failed", exception);
                    state = SessionState.UNAVAILABLE;
                }
                return;
            }
        }
    }

    private void handleCandidate(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            String hello = reader.readLine();
            synchronized (this) {
                if (state == SessionState.CONNECTED) {
                    InkLog.d("event=sender_session_reject_busy remote=" + socket.getRemoteSocketAddress());
                    writer.println("BUSY");
                    socket.close();
                    return;
                }
                if (hello == null || details == null || !hello.equals("HELLO " + details.code)) {
                    InkLog.d("event=sender_session_reject_hello remote=" + socket.getRemoteSocketAddress()
                            + " helloPresent=" + (hello != null)
                            + " detailsPresent=" + (details != null)
                            + " codeMatch=" + (hello != null && details != null && hello.equals("HELLO " + details.code)));
                    writer.println("REJECT");
                    socket.close();
                    return;
                }
                connectedSocket = socket;
                connectedReader = reader;
                connectedWriter = writer;
                sessionId = UUID.randomUUID().toString();
                state = SessionState.CONNECTED;
                discoveryResponder.stop();
                writer.println("CONNECTED " + sessionId);
                InkLog.d("event=sender_session_connected remote=" + socket.getRemoteSocketAddress()
                        + " sessionId=" + sessionId);
            }
        } catch (IOException exception) {
            InkLog.w("event=sender_session_candidate_failed", exception);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String nextCode() {
        return String.format(Locale.US, "%06d", random.nextInt(1000000));
    }

    private void startDiscovery() {
        try {
            AndroidDiscoveryPacketAccess.acquire("sender_responder");
            discoveryResponder.start(new PairingCandidate(details.address, details.port, details.code));
        } catch (SocketException ignored) {
            AndroidDiscoveryPacketAccess.release("sender_responder");
            InkLog.w("event=sender_discovery_start_failed", ignored);
        }
    }

    private void closeConnectedSocket() {
        if (connectedSocket != null) {
            try {
                connectedSocket.close();
            } catch (IOException ignored) {
            }
        }
        connectedSocket = null;
        connectedReader = null;
        connectedWriter = null;
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        serverSocket = null;
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        acceptThread = null;
    }
}
