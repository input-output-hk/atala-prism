import React, { useContext } from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTemplates from './CredentialTemplates';
import { PrismStoreContext } from '../../stores/domain/PrismStore';
import { useTemplatesInit } from '../../hooks/useTemplatesInit';

const CredentialTemplatesContainer = observer(() => {
  const { templateStore } = useContext(PrismStoreContext);
  const { credentialTemplates, templateCategories, isLoading } = templateStore;

  useTemplatesInit();

  const tableProps = {
    credentialTemplates,
    templateCategories,
    isLoading
  };

  return <CredentialTemplates tableProps={tableProps} />;
});

export default CredentialTemplatesContainer;
