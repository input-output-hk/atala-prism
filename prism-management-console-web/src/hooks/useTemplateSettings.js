// eslint-disable-next-line no-unused-vars
import _React, { useReducer } from 'react';
import image0 from '../images/generic-icon-01.svg';
import image1 from '../images/genericUserIcon.svg';

export const getDefaultAttribute = index => ({
  attributeLabel: `Attibute ${index} Label`,
  attributeType: undefined
});

const defaultCredentialBodyLength = 2;

const defaultTemplate = {
  name: '',
  category: undefined,
  layout: 0,
  themeColor: '#FF2D3B',
  backgroundColor: '#0C8762',
  image0,
  image1,
  credentialTitle: 'Title',
  credentialSubtitle: 'Subtitle',
  credentialBody: new Array(defaultCredentialBodyLength)
    .fill(undefined)
    .map((_, index) => getDefaultAttribute(index))
};

// action types:
export const UPDATE_FIELDS = 'UPDATE_FIELDS';

// reducer:
const TemplateReducer = (templateSettings, { type, payload }) => {
  switch (type) {
    case UPDATE_FIELDS:
      return {
        ...templateSettings,
        ...payload
      };
    default:
      return templateSettings;
  }
};

export const useTemplateSettings = () => {
  const [templateSettings, setTemplateSettings] = useReducer(TemplateReducer, defaultTemplate);

  return [templateSettings, setTemplateSettings];
};
