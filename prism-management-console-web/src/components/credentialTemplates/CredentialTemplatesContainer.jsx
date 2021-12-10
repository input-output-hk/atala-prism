import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTemplates from './CredentialTemplates';
import { useTemplatesPageStore } from '../../hooks/useTemplatesPageStore';

const CredentialTemplatesContainer = observer(() => {
  const { init } = useTemplatesPageStore();

  useEffect(() => {
    init();
  }, [init]);

  return <CredentialTemplates />;
});

export default CredentialTemplatesContainer;
