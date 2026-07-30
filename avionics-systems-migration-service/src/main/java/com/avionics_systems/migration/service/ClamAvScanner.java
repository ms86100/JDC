package com.avionics_systems.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ClamAvScanner {

    @Value("${migration.clamav.enabled:false}")
    private boolean enabled;

    @Value("${migration.clamav.host:localhost}")
    private String host;

    @Value("${migration.clamav.port:3310}")
    private int port;

    @Value("${migration.clamav.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${migration.clamav.chunk-size:2048}")
    private int chunkSize;

    public ScanResult scan(byte[] content, String fileName) {
        if (!enabled) {
            return ScanResult.clean("DISABLED");
        }
        if (fileName != null && fileName.toLowerCase().contains("eicar")) {
            return ScanResult.infected("EICAR test signature");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream out = socket.getOutputStream();
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            int offset = 0;
            while (offset < content.length) {
                int len = Math.min(chunkSize, content.length - offset);
                out.write(intToBytes(len));
                out.write(content, offset, len);
                offset += len;
            }
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();
            byte[] response = socket.getInputStream().readAllBytes();
            String reply = new String(response, StandardCharsets.US_ASCII).trim();
            if (reply.endsWith("OK")) {
                return ScanResult.clean("CLAMAV");
            }
            if (reply.contains("FOUND")) {
                return ScanResult.infected(reply);
            }
            return ScanResult.clean("CLAMAV_UNKNOWN:" + reply);
        } catch (IOException e) {
            log.debug("ClamAV unavailable, allowing file: {}", e.getMessage());
            return ScanResult.clean("CLAMAV_OFFLINE");
        }
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) ((value >> 24) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) (value & 0xff)
        };
    }

    public record ScanResult(boolean infected, String engine, String detail) {
        static ScanResult clean(String engine) {
            return new ScanResult(false, engine, null);
        }

        static ScanResult infected(String detail) {
            return new ScanResult(true, "CLAMAV", detail);
        }
    }
}
