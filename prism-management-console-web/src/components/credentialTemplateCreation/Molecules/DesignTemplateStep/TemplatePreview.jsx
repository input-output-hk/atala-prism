import React from 'react';
import { observer } from 'mobx-react-lite';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';

const TemplatePreview = observer(() => {
  const { preview } = useTemplateCreationStore();

  return <CredentialsViewer credentialViews={[preview]} showBrowseControls={false} />;
});

export default TemplatePreview;
