package io.iohk.atala.prism.crypto.japi;

import io.iohk.atala.prism.crypto.AndroidEC$;
import io.iohk.atala.prism.crypto.EC$;

import java.math.BigInteger;
import java.util.Arrays;

public interface EC {
    public static EC getInstance(CryptoProvider provider) {
        switch (provider) {
            case JVM:
                return new ECFacade(EC$.MODULE$);
            case Android:
                return new ECFacade(AndroidEC$.MODULE$);
            default:
                throw new IllegalArgumentException(
                        String.format("Unexpected provider %s, available types are %s",
                                provider, Arrays.toString(CryptoProvider.values())));
        }
    }

    /**
     * Generates a P-256k/secp256k1/prime256v1 key-pair.
     */
    ECKeyPair generateKeyPair();

    /**
     * Returns the key-pair represented by the given private key's `D` as byte array.
     */
    ECKeyPair toKeyPairFromPrivateKey(byte[] d);

    /**
     * Returns the key-pair represented by the given private key's `D` as number.
     */
    ECKeyPair toKeyPairFromPrivateKey(BigInteger d);

    /**
     * Returns the private key represented by the given byte array.
     */
    ECPrivateKey toPrivateKey(byte[] d);

    /**
     * Returns the private key represented by the given number.
     */
    ECPrivateKey toPrivateKey(BigInteger d);

    /**
     * Returns the public key represented by the given encoded byte array.
     */
    ECPublicKey toPublicKey(byte[] encoded);

    /**
     * Returns the public key represented by the given coordinates as byte arrays.
     */
    ECPublicKey toPublicKey(byte[] x, byte[] y);

    /**
     * Returns the public key represented by the given coordinates.
     */
    ECPublicKey toPublicKey(BigInteger x, BigInteger y);

    /**
     * Returns the public key represented by the given private key's `D` as byte array.
     */
    ECPublicKey toPublicKeyFromPrivateKey(byte[] d);

    /**
     * Returns the public key represented by the given private key's `D` as number.
     */
    ECPublicKey toPublicKeyFromPrivateKey(BigInteger d);

    /**
     * Signs the given text with the given private key.
     */
    ECSignature sign(String text, ECPrivateKey privateKey);

    /**
     * Signs the given data with the given private key.
     */
    ECSignature sign(byte[] data, ECPrivateKey privateKey);

    /**
     * Verifies whether the given text matches the given signature with the given public key.
     */
    boolean verify(String text, ECPublicKey publicKey, ECSignature signature);

    /**
     * Verifies whether the given data matches the given signature with the given public key.
     */
    boolean verify(byte[] data, ECPublicKey publicKey, ECSignature signature);
}
