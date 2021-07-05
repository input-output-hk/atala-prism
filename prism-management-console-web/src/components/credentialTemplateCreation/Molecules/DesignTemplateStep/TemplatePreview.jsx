import React, { useEffect, useState } from 'react';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';
import { useTemplateContext } from '../../../providers/TemplateContext';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';

const placeholders = {
  themeColor: '{{themeColor}}',
  backgroundColor: '{{backgroundColor}}',
  image0: '{{image0}}',
  image1: '{{image1}}',
  credentialTitle: '{{credentialTitle}}',
  credentialSubtitle: '{{credentialSubtitle}}',
  attributeLabel: '{{attributeLabel}}',
  attributeType: '{{attributeType}}'
};

const replacePlaceholdersFromObject = (html, ph, data) =>
  Object.keys(ph).reduce((template, key) => template.replace(ph[key], data[key]), html);

const updateHeader = (htmlTemplateHeader, currentSettings) =>
  replacePlaceholdersFromObject(htmlTemplateHeader, placeholders, currentSettings);

const updateBody = (htmlTemplateBody, currentSettings) => {
  const filledTemplate = fillBody(htmlTemplateBody, currentSettings);
  return replacePlaceholdersFromObject(filledTemplate, placeholders, currentSettings);
};

const fillBody = (htmlTemplateBody, currentSettings) => {
  const bodyParts = currentSettings?.credentialBody?.map(attribute =>
    replacePlaceholdersFromObject(htmlTemplateBody, placeholders, attribute)
  );
  return bodyParts.reduce((compilation, part) => compilation.concat(part), '');
};

const getBase64 = file =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });

const TemplatePreview = () => {
  const { templateSettings } = useTemplateContext();
  const { form } = useTemplateContext();
  const [imageOverwrites, setImagesOverwrites] = useState();
  const [displayHtml, setDisplayHtml] = useState('');

  useEffect(() => {
    // FIXME: any better solution to this forced update? better names than image0-1
    const updateImages = async ({ backgroundHeader, iconHeader }) => {
      if (!backgroundHeader && !iconHeader) return;
      const image0 =
        backgroundHeader?.length && (await getBase64(backgroundHeader[0].originFileObj));
      const image1 = iconHeader?.length && (await getBase64(iconHeader[0].originFileObj));

      setImagesOverwrites({ ...(image0 && { image0 }), ...(image1 && { image1 }) });
    };

    updateImages(templateSettings);
  }, [templateSettings]);

  useEffect(() => {
    const formValues = form.getFieldsValue();

    const currentConfig = { ...templateSettings, ...formValues, ...imageOverwrites };

    // FIXME: remove once fully debugged
    // eslint-disable-next-line no-magic-numbers
    // message.info(JSON.stringify(currentConfig, null, 2));

    const htmlTemplate = templateLayouts[currentConfig.layout];
    const htmlTemplateHeader = htmlTemplate.header;
    const htmlTemplateBody = htmlTemplate.body;

    const configuredHeader = updateHeader(htmlTemplateHeader, currentConfig);
    const configuredBody = updateBody(htmlTemplateBody, currentConfig);
    const mergedHtml = replacePlaceholdersFromObject(
      configuredHeader,
      { attributes: '{{#attributes}}' },
      {
        attributes: configuredBody
      }
    );

    setDisplayHtml(mergedHtml);
  }, [templateSettings, form, imageOverwrites]);

  return <CredentialsViewer credentialViews={[displayHtml]} showBrowseControls={false} />;
};

export default TemplatePreview;
