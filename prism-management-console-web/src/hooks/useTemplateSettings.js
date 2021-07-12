// eslint-disable-next-line no-unused-vars
import _React, { useReducer } from 'react';
import _ from 'lodash';
import embeddedCompanyLogo from '../images/generic-icon-01.svg';
import embeddedUserIcon from '../images/genericUserIcon.svg';

export const getNewDynamicAttribute = attributes => {
  const index = attributes.length;
  const dynamicAttributeIndex = attributes.filter(attr => !attr.isFixedText).length;
  return {
    attributeLabel: `Attribute ${dynamicAttributeIndex + 1}`,
    attributeType: undefined,
    dynamicAttributeIndex,
    key: index
  };
};

export const getNewFixedTextAttribute = attributes => {
  const index = attributes.length;
  const textAttributeIndex = attributes.filter(attr => attr.isFixedText).length;
  return {
    text: `Text Field ${textAttributeIndex + 1}`,
    isFixedText: true,
    textAttributeIndex,
    key: index
  };
};

const getDefaultAttribute = index => ({
  attributeLabel: `Attribute ${index + 1}`,
  attributeType: undefined,
  dynamicAttributeIndex: index,
  key: index
});

const defaultCredentialBodyLength = 2;

const defaultTemplate = {
  name: '',
  category: undefined,
  layout: 0,
  themeColor: '#D8D8D8',
  backgroundColor: '#FFFFFF',
  embeddedCompanyLogo,
  embeddedUserIcon,
  credentialTitle: 'Title',
  credentialSubtitle: 'Subtitle',
  credentialBody: new Array(defaultCredentialBodyLength)
    .fill(undefined)
    .map((_item, index) => getDefaultAttribute(index))
};

// action types:
export const UPDATE_FIELDS = 'UPDATE_FIELDS';

const insertFormChangeIntoArray = (change, oldArray) => {
  // change can either be...
  // - undefined: w hen there's no change => return the unchanged array
  if (!change) return oldArray;

  // - A dense array: when there's a change in sort => return the sorted array
  if (_.compact(change).length === oldArray.length) return change;

  // - A sparse array: on any other change event => merge both arrays
  const changeArray = Array.from(change); // casting to array because it's a sparse array
  const newPartialArray = changeArray.map((ch, index) => Object.assign({ ...oldArray[index] }, ch));
  const oldArrayTail = oldArray.slice(newPartialArray.length);
  const newArray = newPartialArray.concat(oldArrayTail);
  return newArray;
};

// reducer:
const TemplateReducer = (templateSettings, { type, payload }) => {
  const newCredentialBody = insertFormChangeIntoArray(
    payload.credentialBody,
    templateSettings.credentialBody
  );
  switch (type) {
    case UPDATE_FIELDS:
      return {
        ...templateSettings,
        ...payload,
        credentialBody: newCredentialBody
      };
    default:
      return templateSettings;
  }
};

export const useTemplateSettings = () => {
  const [templateSettings, setTemplateSettings] = useReducer(TemplateReducer, defaultTemplate);

  return [templateSettings, setTemplateSettings];
};
