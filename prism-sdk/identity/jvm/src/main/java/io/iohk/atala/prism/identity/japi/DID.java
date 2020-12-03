package io.iohk.atala.prism.identity.japi;

import java.util.Optional;

public interface DID {
    String getValue();

    boolean isLongForm();

    boolean isCanonicalForm();

    Optional<String> getSuffix();

    Optional<String> getCanonicalSuffix();
}
