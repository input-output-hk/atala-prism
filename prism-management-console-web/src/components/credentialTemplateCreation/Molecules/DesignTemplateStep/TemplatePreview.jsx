import React, { useEffect, useState } from 'react';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateContext } from '../../../providers/TemplateContext';
import {
  getContrastColorSettings,
  configureHtmlTemplate,
  updateImages
} from '../../../../helpers/templateLayouts/templates';

const TemplatePreview = () => {
  const { templateSettings, form, setTemplatePreview } = useTemplateContext();
  const [imageOverwrites, setImagesOverwrites] = useState();
  const [displayHtml, setDisplayHtml] = useState('');

  useEffect(() => {
    updateImages(templateSettings, setImagesOverwrites);
  }, [templateSettings]);

  useEffect(() => {
    const contrastColorSettings = getContrastColorSettings(templateSettings);
    const currentConfig = {
      ...templateSettings,
      ...imageOverwrites,
      ...contrastColorSettings
    };

    const configuredHtmlTemplate = configureHtmlTemplate(currentConfig.layout, currentConfig);

    setDisplayHtml(configuredHtmlTemplate);
    setTemplatePreview(configuredHtmlTemplate);
  }, [templateSettings, form, imageOverwrites, setTemplatePreview]);

  return <CredentialsViewer credentialViews={[displayHtml]} showBrowseControls={false} />;
};

export default TemplatePreview;
