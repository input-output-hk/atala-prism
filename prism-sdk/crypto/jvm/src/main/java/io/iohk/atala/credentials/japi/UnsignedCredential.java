package io.iohk.atala.credentials.japi;

public class UnsignedCredential {
    private final byte[] bytes;

    public UnsignedCredential(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
