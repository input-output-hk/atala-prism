package io.iohk.atala.prism.crypto.japi;

import io.iohk.atala.prism.compat.AsJavaConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents derivation path in BIP 32 protocol */
public class DerivationPath {
    final private List<DerivationAxis> axes;

    public DerivationPath(List<DerivationAxis> axes) {
        this.axes = new ArrayList<>(axes);
    }

    public DerivationPath(DerivationPath path, DerivationAxis newAxis) {
        ArrayList<DerivationAxis> _axes = new ArrayList<>(path.axes);
        _axes.add(newAxis);
        this.axes = Collections.unmodifiableList(_axes);
    }


    /**
     * Creates child derivation path for given index, hardened or not */
    public DerivationPath derive(DerivationAxis axis) {
        return new DerivationPath(this, axis);
    }

    public List<DerivationAxis> getAxes() {
        return axes;
    }

    public String toString() {
        scala.collection.immutable.VectorBuilder<io.iohk.atala.prism.crypto.DerivationAxis> buf =
                new scala.collection.immutable.VectorBuilder<>();

        for(DerivationAxis axis : axes) {
            buf.$plus$eq(new io.iohk.atala.prism.crypto.DerivationAxis(axis.getNumber()));
        }

        return io.iohk.atala.prism.crypto.DerivationPath.apply(buf.result()).toString();
    }

    /**
     * Parses string representation of derivation path

     * @param path Path to parse in format m/axis1/axis2/.../axisn where all axes are number between 0 and 2^31^ - 1 and
     *             optionally a ' added after to mark hardened axis e.g. m/21/37'/0
     */
    public static DerivationPath parse(String path) {
        ArrayList<DerivationAxis> _axes = new ArrayList<>();

        Iterable<io.iohk.atala.prism.crypto.DerivationAxis> iterable =
            AsJavaConverter.asJavaIterable(
                io.iohk.atala.prism.crypto.DerivationPath.apply(path).axes().toIterable()
            );
        
        for (io.iohk.atala.prism.crypto.DerivationAxis scalaAxis : iterable) {
            _axes.add(DerivationAxis.raw(scalaAxis.i()));
        }

        return new DerivationPath(_axes);
    }
}
