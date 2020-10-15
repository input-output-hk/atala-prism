package io.iohk.atala.prism.credentials.japi;

public class UnsignedCredential {
    private final byte[] bytes;

    public UnsignedCredential(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
