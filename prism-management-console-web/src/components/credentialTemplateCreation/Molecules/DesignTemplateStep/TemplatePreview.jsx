import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateContext } from '../../../providers/TemplateContext';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';

const TemplatePreview = () => {
  const { templateSettings } = useTemplateContext();
  const [imageOverwrites, setImagesOverwrites] = useState();

  useEffect(() => {
    const getBase64 = file =>
      new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
      });

    // FIXME: any better solution to this forced update?
    const updateImages = async ({ backgroundHeader, iconHeader, image0, image1 }) => {
      const updatedImage0 = backgroundHeader?.length
        ? await getBase64(backgroundHeader[0].originFileObj)
        : image0;
      const updatedImage1 = iconHeader?.length
        ? await getBase64(iconHeader[0].originFileObj)
        : image1;
      setImagesOverwrites({ image0: updatedImage0, image1: updatedImage1 });
    };

    // eslint-disable-next-line no-magic-numbers
    message.info(JSON.stringify(templateSettings, null, 2));
    updateImages(templateSettings);
  }, [templateSettings]);

  const replacePlaceholdersFromObject = (html, placeholders, data) =>
    Object.keys(placeholders).reduce(
      (template, key) => template.replace(placeholders[key], data[key]),
      html
    );

  const configureHtmlTemplate = (template, settings) => {
    const placeholders = {
      themeColor: '{{themeColor}}',
      backgroundColor: '{{backgroundColor}}',
      image0: '{{image0}}',
      image1: '{{image1}}'
    };

    return replacePlaceholdersFromObject(template, placeholders, {
      ...settings,
      ...imageOverwrites
    });
  };

  const htmlTemplate = templateLayouts[templateSettings.layout]?.html;
  const htmlCredential = configureHtmlTemplate(htmlTemplate, templateSettings);

  return <CredentialsViewer credentialViews={[htmlCredential]} showBrowseControls={false} />;
};

export default TemplatePreview;
