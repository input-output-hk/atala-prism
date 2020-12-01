package io.iohk.atala.prism.identity.japi;

import io.iohk.atala.prism.compat.AsJavaConverter;
import io.iohk.atala.prism.compat.AsScalaConverter;
import io.iohk.atala.prism.crypto.japi.ECPublicKey;
import io.iohk.atala.prism.crypto.japi.ECPublicKeyFacade;

import java.util.Optional;

public class DIDFactory {
  public static Optional<DID> fromString(String string) {
    return AsJavaConverter.asJavaOptional(
        io.iohk.atala.prism.identity.DID.fromString(string).map(DIDFacade::new)
    );
  }

  public static DID unsafeFromString(String string) {
    return new DIDFacade(io.iohk.atala.prism.identity.DID.unsafeFromString(string));
  }

  public static DID buildPrismDID(String stateHash) {
    return new DIDFacade(
        io.iohk.atala.prism.identity.DID.buildPrismDID(
            stateHash,
            AsScalaConverter.asScalaOption(Optional.empty())
        )
    );
  }

  public static DID buildPrismDID(String stateHash, String encodedState) {
    return new DIDFacade(
        io.iohk.atala.prism.identity.DID.buildPrismDID(
            stateHash,
            AsScalaConverter.asScalaOption(Optional.of(encodedState))
        )
    );
  }

  public static DID createUnpublishedDID(ECPublicKey masterKey) {
    return new DIDFacade(
        io.iohk.atala.prism.identity.DID.createUnpublishedDID(
            ((ECPublicKeyFacade) masterKey).publicKey()
        )
    );
  } 
}
