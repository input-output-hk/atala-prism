import _ from 'lodash';
import { getContrastColor } from '../colors';

import { template0 } from './template0';
import { template1 } from './template1';
import { template2 } from './template2';
import { template3 } from './template3';
import { template4 } from './template4';

import TemplateLayoutImage0 from '../../images/TemplateLayout_0.svg';
import TemplateLayoutImage1 from '../../images/TemplateLayout_1.svg';
import TemplateLayoutImage2 from '../../images/TemplateLayout_2.svg';
import TemplateLayoutImage3 from '../../images/TemplateLayout_3.svg';
import TemplateLayoutImage4 from '../../images/TemplateLayout_4.svg';

export const templateLayouts = [
  {
    thumb: TemplateLayoutImage0,
    images: ['userIcon'],
    ...template0
  },
  {
    thumb: TemplateLayoutImage1,
    images: ['companyIcon'],

    ...template1
  },
  {
    thumb: TemplateLayoutImage2,
    images: ['companyIcon'],
    ...template2
  },
  {
    thumb: TemplateLayoutImage3,
    images: ['companyIcon'],
    ...template3
  },
  {
    thumb: TemplateLayoutImage4,
    images: ['companyIcon', 'userIcon'],
    ...template4
  }
];

export const placeholders = {
  themeColor: '{{themeColor}}',
  backgroundColor: '{{backgroundColor}}',
  contrastThemeColor: '{{contrastThemeColor}}',
  contrastBackgroundColor: '{{contrastBackgroundColor}}',
  image0: '{{image0}}',
  image1: '{{image1}}',
  credentialTitle: '{{credentialTitle}}',
  credentialSubtitle: '{{credentialSubtitle}}',
  attributeLabel: '{{attributeLabel}}',
  attributeLabelPlaceholder: '{{attributeLabelPlaceholder}}',
  attributeType: '{{attributeType}}',
  text: '{{text}}'
};

export const configureHtmlTemplate = (tempalteId, currentConfig) => {
  const htmlTemplate = templateLayouts[tempalteId];
  const configuredHeader = updateHeader(htmlTemplate, currentConfig);
  const configuredBody = updateBody(htmlTemplate, currentConfig);
  const mergedHtml = replacePlaceholdersFromObject(
    configuredHeader,
    { attributes: '{{#attributes}}' },
    {
      attributes: configuredBody
    }
  );

  return mergedHtml;
};

// replaces {{placeholders}} with object value (unless the value is undefined).
const replacePlaceholdersFromObject = (html, ph, data) =>
  Object.keys(ph).reduce(
    (template, key) => template.replaceAll(ph[key], data[key] || ph[key]),
    html
  );

const updateHeader = ({ header }, currentSettings) =>
  replacePlaceholdersFromObject(header, placeholders, currentSettings);

const updateBody = (htmlTemplateBody, currentSettings) => {
  const filledTemplate = fillBody(htmlTemplateBody, currentSettings);
  return replacePlaceholdersFromObject(filledTemplate, placeholders, currentSettings);
};

const fillBody = ({ dynamicAttribute, fixedText }, currentSettings) => {
  const bodyParts = currentSettings?.credentialBody?.map(attribute => {
    const parsedAttribute = {
      ...attribute,
      attributeLabelPlaceholder: _.camelCase(attribute.attributeLabel)
    };
    return replacePlaceholdersFromObject(
      attribute.isFixedText ? fixedText : dynamicAttribute,
      placeholders,
      parsedAttribute
    );
  });
  return bodyParts.reduce((compilation, part) => compilation.concat(part), '');
};

// FIXME: any better solution to this forced update? better names than image0-1
export const updateImages = async ({ companyIcon, userIcon }, setImagesOverwrites) => {
  if (!companyIcon && !userIcon) return;
  const image0 = companyIcon?.length && (await getBase64(companyIcon[0].originFileObj));
  const image1 = userIcon?.length && (await getBase64(userIcon[0].originFileObj));

  setImagesOverwrites({ ...(image0 && { image0 }), ...(image1 && { image1 }) });
};

const getBase64 = file =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });

export const getContrastColorSettings = ({ themeColor, backgroundColor }) => ({
  contrastThemeColor: getContrastColor(themeColor),
  contrastBackgroundColor: getContrastColor(backgroundColor)
});
