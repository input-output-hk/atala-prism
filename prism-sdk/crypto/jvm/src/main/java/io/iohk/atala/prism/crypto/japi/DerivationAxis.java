package io.iohk.atala.prism.crypto.japi;


/**
 * Represent an axis on BIP 32 key derivation path
 *
 * BIP 32 standard defines how keys can be derived from another one for index between 0 and 2^32^ - 1, where
 * indices between 0 and 2^31^ - 1 are called normal indices and between 2^31^ and 2^32^ - 1 hardened indices.
 * Natural way to represent such thing is unsigned 32-bit integer, but JVM (and Scala) doesn't support such
 * data type. That is why signed 32-bit is used instead, with the same bit representation. In other words
 * unsigned index used here is equivalent to canonical, unsigned one modulo 2^32^.
 *
 * Implementation details are mostly hidden from the user, so user can either choose to create a normal
 * axis, providing number between 0 and 2^31^ - 1 or hardened one, providing a number from the same range.
 */
public class DerivationAxis {
    private int i;

    private DerivationAxis(int axis) {
        this.i = axis;
    }

    public int getI() {
        return this.i;
    }

    /**
     * Checks if the axis is hardened */
    public boolean isHardened() {
        return io.iohk.atala.prism.crypto.DerivationAxis.isHardened$extension(i);
    }

    /**
     * Returns number corresponding to the axis (different for index), always between 0 and 2^31^ */
    public int getNumber() {
        return io.iohk.atala.prism.crypto.DerivationAxis.number$extension(i);
    }

    public String axisToString() {
        return io.iohk.atala.prism.crypto.DerivationAxis.axisToString$extension(i);
    }

    public static DerivationAxis raw(int i) {
        return new DerivationAxis(i);
    }

    /**
     * Creates normal (non-hardened) axis
     *
     * @param num number corresponding to the axis, must be between 0 and 2^31^ - 1
     */
    public static DerivationAxis normal(int num) {
        return new DerivationAxis(io.iohk.atala.prism.crypto.DerivationAxis.normal(num));
    }

    public static DerivationAxis hardened(int num) {
        return new DerivationAxis(io.iohk.atala.prism.crypto.DerivationAxis.hardened(num));
    }
}
