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
  attributeType: '{{attributeType}}',
  text: '{{text}}'
};

const replacePlaceholdersFromObject = (html, ph, data) =>
  Object.keys(ph).reduce((template, key) => template.replaceAll(ph[key], data[key]), html);

const updateHeader = ({ header }, currentSettings) =>
  replacePlaceholdersFromObject(header, placeholders, currentSettings);

const updateBody = (htmlTemplateBody, currentSettings) => {
  const filledTemplate = fillBody(htmlTemplateBody, currentSettings);
  return replacePlaceholdersFromObject(filledTemplate, placeholders, currentSettings);
};

const fillBody = ({ dynamicAttribute, fixedText }, currentSettings) => {
  const bodyParts = currentSettings?.credentialBody?.map(attribute =>
    replacePlaceholdersFromObject(
      attribute.isFixedText ? fixedText : dynamicAttribute,
      placeholders,
      attribute
    )
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
    const updateImages = async ({ companyIcon, userIcon }) => {
      if (!companyIcon && !userIcon) return;
      const image0 = companyIcon?.length && (await getBase64(companyIcon[0].originFileObj));
      const image1 = userIcon?.length && (await getBase64(userIcon[0].originFileObj));

      setImagesOverwrites({ ...(image0 && { image0 }), ...(image1 && { image1 }) });
    };

    updateImages(templateSettings);
  }, [templateSettings]);

  useEffect(() => {
    const formValues = form.getFieldsValue();

    const currentConfig = { ...templateSettings, ...formValues, ...imageOverwrites };

    const htmlTemplate = templateLayouts[currentConfig.layout];
    // const htmlTemplateHeader = htmlTemplate.header;
    // const htmlTemplateFixedText = htmlTemplate.fixedText;
    // const htmlTemplateDynamicAttribute = htmlTemplate.attribute;

    const configuredHeader = updateHeader(htmlTemplate, currentConfig);
    const configuredBody = updateBody(htmlTemplate, currentConfig);
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
