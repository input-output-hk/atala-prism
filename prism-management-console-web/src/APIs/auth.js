function DIDBased(configs, wallet) {
  // FIXME: improve catching of wallet disconnection.
  // The timeout causes other issues when calls take longer to complete.
  // The timeout is disabled until a better fix is implemented.
  const getMetadata = async (unsignedRequest, timeout) => {
    const {
      encodedNonce,
      encodedSignature,
      did,
      didKeyId,
      sessionError
    } = await wallet.signMessage(unsignedRequest, timeout);

    return {
      metadata: {
        requestNonce: encodedNonce,
        didSignature: encodedSignature,
        did,
        didKeyId
      },
      sessionError
    };
  };

  return { getMetadata };
}

export { DIDBased };
