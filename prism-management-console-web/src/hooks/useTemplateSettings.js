// eslint-disable-next-line no-unused-vars
import _React, { useReducer } from 'react';
import _ from 'lodash';
import { defaultTemplateSketch } from '../helpers/templateHelpers';

// action types:
export const UPDATE_FIELDS = 'UPDATE_FIELDS';

const insertFormChangeIntoArray = (change, oldArray) => {
  // change can either be...
  // - undefined: w hen there's no change => return the unchanged array
  if (!change) return oldArray;

  // - A dense array: when there's a change in sort => return the sorted array
  if (_.compact(change).length === change.length) return change;

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
  const [templateSettings, setTemplateSettings] = useReducer(
    TemplateReducer,
    defaultTemplateSketch
  );

  return [templateSettings, setTemplateSettings];
};
