function DIDBased(configs, wallet) {
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
