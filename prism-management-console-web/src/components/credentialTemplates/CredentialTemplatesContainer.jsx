import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTemplates from './CredentialTemplates';
import { useTemplatePageStore } from '../../hooks/useTemplatesPageStore';

const CredentialTemplatesContainer = observer(() => {
  const { initTemplateStore } = useTemplatePageStore();

  useEffect(() => {
    initTemplateStore();
  }, [initTemplateStore]);

  return <CredentialTemplates />;
});

export default CredentialTemplatesContainer;
