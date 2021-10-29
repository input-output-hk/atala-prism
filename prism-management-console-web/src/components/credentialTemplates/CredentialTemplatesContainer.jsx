import React from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTemplates from './CredentialTemplates';
import { useTemplateStore, useTemplateUiState } from '../../hooks/useTemplateStore';

const CredentialTemplatesContainer = observer(() => {
  useTemplateStore({ fetch: true });
  useTemplateUiState({ reset: true });

  return <CredentialTemplates />;
});

export default CredentialTemplatesContainer;
