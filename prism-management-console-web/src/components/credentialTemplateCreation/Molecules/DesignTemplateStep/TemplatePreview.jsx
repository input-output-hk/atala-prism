import React, { useEffect } from 'react';
import { message } from 'antd';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateContext } from '../../../providers/TemplateContext';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';

const TemplatePreview = () => {
  const { templateSettings } = useTemplateContext();

  useEffect(() => {
    // eslint-disable-next-line no-magic-numbers
    message.info(JSON.stringify(templateSettings, null, 2));
  }, [templateSettings]);

  // CredentialsViewer expects an array as credentialViews prop, so we send an array of length one.
  const credentialHtml = [templateLayouts[templateSettings.layout]?.html];

  return <CredentialsViewer credentialViews={credentialHtml} showBrowseControls={false} />;
};

TemplatePreview.propTypes = {};

export default TemplatePreview;
