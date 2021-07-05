import React, { useEffect, useState } from 'react';
import { message } from 'antd';
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

const TemplatePreview = () => {
  const { templateSettings } = useTemplateContext();
  const { form } = useTemplateContext();
  const [imageOverwrites, setImagesOverwrites] = useState();
  const [displayHtml, setDisplayHtml] = useState('');

  useEffect(() => {
    const getBase64 = file =>
      new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
      });

    // FIXME: any better solution to this forced update? better names than image0-1
    const updateImages = async ({ backgroundHeader, iconHeader, image0, image1 }) => {
      const updatedImage0 = backgroundHeader?.length
        ? await getBase64(backgroundHeader[0].originFileObj)
        : image0;
      const updatedImage1 = iconHeader?.length
        ? await getBase64(iconHeader[0].originFileObj)
        : image1;
      setImagesOverwrites({ image0: updatedImage0, image1: updatedImage1 });
    };

    const values = form.getFieldsValue();
    // eslint-disable-next-line no-magic-numbers
    message.info(JSON.stringify(values, null, 2));
    const currentSettings =
      values && Object.keys(values).length === 0 && values.constructor === Object
        ? templateSettings
        : values;

    updateImages(templateSettings);
    const configuredBody = updateBody(currentSettings);
    const configuredHeader = updateHeader(currentSettings);
    const mergedHtml = replacePlaceholdersFromObject(
      configuredHeader,
      { attributes: '{{#attributes}}' },
      {
        attributes: configuredBody
      }
    );

    setDisplayHtml(mergedHtml);
  }, [templateSettings, form]);

  const updateBody = currentSettings => {
    const htmlTemplateBody = templateLayouts[currentSettings.layout]?.body;
    const configuredBody = configureBody(htmlTemplateBody, currentSettings);

    return configuredBody;
  };

  const updateHeader = currentSettings => {
    const htmlTemplateHeader = templateLayouts[currentSettings.layout]?.header;
    const configuredHeader = configureHeader(htmlTemplateHeader, currentSettings);

    return configuredHeader;
  };

  const fillBody = (template, settings) => {
    const bodyParts = settings?.credentialBody?.map(attribute =>
      replacePlaceholdersFromObject(template, placeholders, {
        ...attribute,
        ...imageOverwrites
      })
    );
    return bodyParts.reduce((compilation, part) => compilation.concat(part), '');
  };

  const configureHeader = (template, settings) =>
    replacePlaceholdersFromObject(template, placeholders, {
      ...settings,
      ...imageOverwrites
    });

  const configureBody = (template, settings) => {
    const filledTemplate = fillBody(template, settings);
    return replacePlaceholdersFromObject(filledTemplate, placeholders, {
      ...settings,
      ...imageOverwrites
    });
  };

  // const htmlTemplateHeader = templateLayouts[templateSettings.layout]?.header;
  // const htmlTemplateBody = templateLayouts[templateSettings.layout]?.body;
  // const configuredHeader = configureHeader(htmlTemplateHeader, templateSettings);
  // const configuredBody = configureBody(htmlTemplateBody, currentSettings);

  return <CredentialsViewer credentialViews={[displayHtml]} showBrowseControls={false} />;
};

export default TemplatePreview;
