import { useCallback, useEffect, useState } from 'react';

export const useCredentialTypes = credentialTypesManager => {
  const [credentialTypes, setCredentialTypes] = useState([]);

  const getCredentialTypes = useCallback(
    () => credentialTypesManager.getCredentialTypes().then(setCredentialTypes),
    [credentialTypesManager]
  );

  const getCredentialTypeDetails = id => credentialTypesManager.getCredentialTypeDetails(id);

  useEffect(() => {
    getCredentialTypes();
  }, [getCredentialTypes]);

  return { credentialTypes, setCredentialTypes, getCredentialTypeDetails };
};
