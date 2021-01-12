function DIDBased(configs, wallet) {
  const getMetadata = async unsignedRequest => {
    const { encodedNonce, encodedSignature, did, didKeyId } = await wallet.signMessage(
      unsignedRequest
    );

    return {
      requestNonce: encodedNonce,
      didSignature: encodedSignature,
      did,
      didKeyId
    };
  };

  return { getMetadata };
}

export { DIDBased };
