package com.example.inkhelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class LocalSessionClient {
    private static final int SOCKET_TIMEOUT_MILLIS = 10000;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readerThread;
    private volatile SessionState state = SessionState.DISCONNECTED;
    private volatile boolean stopped = true;
    private String sessionId;
    private int connectionGeneration;

    public synchronized SessionState connect(
            String host,
            int port,
            String code,
            InboundNotificationReceiver receiver) {
        return connect(host, port, code, receiver, null);
    }

    public synchronized SessionState connect(
            String host,
            int port,
            String code,
            InboundNotificationReceiver receiver,
            Runnable unavailableListener) {
        InkLog.d("event=receiver_connect_start host=" + host
                + " port=" + port
                + " codeLength=" + (code == null ? 0 : code.trim().length()));
        if (isBlank(host) || isBlank(code) || port <= 0 || receiver == null) {
            state = SessionState.UNAVAILABLE;
            InkLog.d("event=receiver_connect_invalid_input hostBlank=" + isBlank(host)
                    + " codeBlank=" + isBlank(code)
                    + " port=" + port
                    + " receiverPresent=" + (receiver != null)
                    + " state=" + state.name());
            return state;
        }
        if (state == SessionState.CONNECTED && socket != null && socket.isConnected() && !socket.isClosed()) {
            InkLog.d("event=receiver_connect_already_connected host=" + host
                    + " port=" + port
                    + " sessionId=" + sessionId);
            return state;
        }
        stop();
        stopped = false;
        int generation = ++connectionGeneration;
        state = SessionState.CONNECTING;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host.trim(), port), SOCKET_TIMEOUT_MILLIS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
            InkLog.d("event=receiver_connect_socket_connected remote="
                    + socket.getRemoteSocketAddress()
                    + " local=" + socket.getLocalSocketAddress());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("HELLO " + code.trim());
            InkLog.d("event=receiver_connect_hello_sent codeLength=" + code.trim().length());
            String response = reader.readLine();
            if (response == null || !response.startsWith("CONNECTED ")) {
                InkLog.d("event=receiver_connect_rejected response=" + (response == null ? "null" : response)
                        + " stateBeforeMark=" + state.name());
                markUnavailable();
                return state;
            }
            sessionId = response.substring("CONNECTED ".length());
            state = SessionState.CONNECTED;
            socket.setSoTimeout(0);
            InkLog.d("event=receiver_connect_connected sessionId=" + sessionId);
            readerThread = new Thread(
                    () -> readLoop(receiver, generation, unavailableListener),
                    "InkHelperReceiverSession");
            readerThread.setDaemon(true);
            readerThread.start();
            return state;
        } catch (IOException | RuntimeException exception) {
            InkLog.w("event=receiver_connect_failed host=" + host + " port=" + port, exception);
            markUnavailable();
            return state;
        }
    }

    public synchronized void stop() {
        InkLog.d("event=receiver_session_stop previousState=" + state.name());
        stopped = true;
        connectionGeneration++;
        closeSocket();
        state = SessionState.DISCONNECTED;
        sessionId = null;
    }

    public synchronized void markUnavailable() {
        markUnavailableForGeneration(connectionGeneration);
    }

    private synchronized boolean markUnavailableForGeneration(int generation) {
        if (generation != connectionGeneration) {
            InkLog.d("event=receiver_session_ignore_stale_unavailable generation=" + generation
                    + " currentGeneration=" + connectionGeneration);
            return false;
        }
        InkLog.d("event=receiver_session_mark_unavailable stopped=" + stopped + " previousState=" + state.name());
        closeSocket();
        state = stopped ? SessionState.DISCONNECTED : SessionState.UNAVAILABLE;
        sessionId = null;
        return state == SessionState.UNAVAILABLE;
    }

    public SessionState state() {
        return state;
    }

    public String sessionId() {
        return sessionId;
    }

    private void readLoop(InboundNotificationReceiver receiver, int generation, Runnable unavailableListener) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String line = reader.readLine();
                if (line == null) {
                    if (shouldMarkUnavailable(generation)) {
                        InkLog.d("event=receiver_read_loop_remote_closed");
                        notifyUnavailable(markUnavailableForGeneration(generation), unavailableListener);
                    }
                    return;
                }
                handleLine(receiver, line, generation);
            } catch (IOException exception) {
                if (shouldMarkUnavailable(generation)) {
                    InkLog.w("event=receiver_read_loop_failed", exception);
                    notifyUnavailable(markUnavailableForGeneration(generation), unavailableListener);
                }
                return;
            }
        }
    }

    private void notifyUnavailable(boolean becameUnavailable, Runnable unavailableListener) {
        if (becameUnavailable && unavailableListener != null) {
            try {
                unavailableListener.run();
            } catch (RuntimeException exception) {
                InkLog.w("event=receiver_session_unavailable_listener_failed", exception);
            }
        }
    }

    private synchronized boolean shouldMarkUnavailable(int generation) {
        return generation == connectionGeneration && !stopped;
    }

    private void handleLine(InboundNotificationReceiver receiver, String line, int generation) {
        if (generation != connectionGeneration) {
            InkLog.d("event=receiver_read_loop_ignored_stale_line generation=" + generation);
            return;
        }
        if (!line.startsWith("NOTIFICATION ")) {
            InkLog.d("event=receiver_read_loop_ignored_line line=" + line);
            return;
        }
        String[] parts = line.split(" ", 3);
        if (parts.length != 3) {
            return;
        }
        boolean accepted = false;
        try {
            accepted = receiver.receive(sessionId, TransferNotificationCodec.decode(parts[2]));
        } catch (IOException | RuntimeException ignored) {
            accepted = false;
        }
        writer.println((accepted ? "ACK " : "REJECT ") + parts[1]);
        InkLog.d("event=receiver_notification_validated accepted=" + accepted + " transferId=" + parts[1]);
    }

    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        socket = null;
        reader = null;
        writer = null;
        if (readerThread != null) {
            readerThread.interrupt();
        }
        readerThread = null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
