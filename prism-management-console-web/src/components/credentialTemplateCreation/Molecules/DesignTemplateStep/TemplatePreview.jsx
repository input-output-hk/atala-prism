import React from 'react';
import { observer } from 'mobx-react-lite';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';

const TemplatePreview = observer(() => {
  const { preview } = useTemplateSketch();

  return <CredentialsViewer credentialViews={[preview]} showBrowseControls={false} />;
});

export default TemplatePreview;
