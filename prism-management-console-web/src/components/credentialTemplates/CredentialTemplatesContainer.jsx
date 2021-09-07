import React from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTemplates from './CredentialTemplates';
import { useTemplateStore, useTemplateUiState } from '../../hooks/useStore';

const CredentialTemplatesContainer = observer(() => {
  const { credentialTemplates, templateCategories, isLoading } = useTemplateStore({ fetch: true });
  useTemplateUiState({ reset: true });

  const tableProps = {
    credentialTemplates,
    templateCategories,
    isLoading
  };

  return <CredentialTemplates tableProps={tableProps} />;
});

export default CredentialTemplatesContainer;
