package com.hatechnology.apps.core_messaging;

public class SocketHost {
    private String host;
    private int port;

    public SocketHost() {
    }

    public SocketHost(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
