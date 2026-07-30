package com.example.inkhelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public final class PairingDiscoveryResponder {
    public static final int DISCOVERY_PORT = 39485;
    private static final int BUFFER_SIZE = 512;
    private DatagramSocket socket;
    private Thread thread;
    private volatile boolean running;
    private volatile PairingCandidate candidate;

    public synchronized void start(PairingCandidate candidate) throws SocketException {
        stop();
        if (candidate == null || !candidate.isValid()) {
            return;
        }
        this.candidate = candidate;
        socket = new DatagramSocket(DISCOVERY_PORT);
        socket.setBroadcast(true);
        running = true;
        InkLog.d("event=pairing_responder_start address=" + candidate.senderAddress
                + " sessionPort=" + candidate.sessionPort
                + " codeLength=" + candidate.sessionCode.length()
                + " discoveryPort=" + DISCOVERY_PORT);
        thread = new Thread(this::respondLoop, "InkHelperPairingDiscovery");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        socket = null;
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
        InkLog.d("event=pairing_responder_stop");
        candidate = null;
    }

    public boolean isRunning() {
        return running;
    }

    private void respondLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String message = new String(request.getData(), request.getOffset(), request.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                if (!PairingDiscoveryMessage.DISCOVER.equals(message)) {
                    InkLog.d("event=pairing_responder_ignore_request sourceAddress="
                            + request.getAddress().getHostAddress()
                            + " sourcePort=" + request.getPort()
                            + " bytes=" + request.getLength());
                    continue;
                }
                PairingCandidate currentCandidate = candidate;
                if (currentCandidate == null || !currentCandidate.isValid()) {
                    InkLog.d("event=pairing_responder_no_candidate sourceAddress="
                            + request.getAddress().getHostAddress());
                    continue;
                }
                byte[] response = PairingDiscoveryMessage.encodeCandidate(currentCandidate)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                InetAddress responseAddress = request.getAddress();
                DatagramPacket packet = new DatagramPacket(response, response.length, responseAddress, request.getPort());
                socket.send(packet);
                InkLog.d("event=pairing_responder_response_sent targetAddress="
                        + responseAddress.getHostAddress()
                        + " targetPort=" + request.getPort()
                        + " sessionPort=" + currentCandidate.sessionPort
                        + " codeLength=" + currentCandidate.sessionCode.length());
            } catch (IOException exception) {
                if (running) {
                    InkLog.w("event=pairing_responder_failed", exception);
                    stop();
                }
                return;
            }
        }
    }
}
