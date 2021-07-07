import React, { useEffect, useState } from 'react';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateContext } from '../../../providers/TemplateContext';
import {
  getContrastColorSettings,
  configureHtmlTemplate,
  updateImages
} from '../../../../helpers/templateLayouts/templates';

const TemplatePreview = () => {
  const { templateSettings } = useTemplateContext();
  const { form } = useTemplateContext();
  const [imageOverwrites, setImagesOverwrites] = useState();
  const [displayHtml, setDisplayHtml] = useState('');

  useEffect(() => {
    updateImages(templateSettings, setImagesOverwrites);
  }, [templateSettings]);

  useEffect(() => {
    const formValues = form.getFieldsValue();
    const contrastColorSettings = getContrastColorSettings(templateSettings);
    const currentConfig = {
      ...templateSettings,
      ...formValues,
      ...imageOverwrites,
      ...contrastColorSettings
    };

    const configuredHtmlTemplate = configureHtmlTemplate(currentConfig.layout, currentConfig);

    setDisplayHtml(configuredHtmlTemplate);
  }, [templateSettings, form, imageOverwrites]);

  return <CredentialsViewer credentialViews={[displayHtml]} showBrowseControls={false} />;
};

export default TemplatePreview;
