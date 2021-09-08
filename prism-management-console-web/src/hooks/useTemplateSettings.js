// eslint-disable-next-line no-unused-vars
import _React, { useReducer } from 'react';
import { defaultTemplateSketch, insertFormChangeIntoArray } from '../helpers/templateHelpers';

// action types:
export const UPDATE_FIELDS = 'UPDATE_FIELDS';
export const UPDATE_CREDENTIAL_BODY = 'UPDATE_CREDENTIAL_BODY';

// reducer:
const TemplateReducer = (templateSettings, { type, payload }) => {
  switch (type) {
    case UPDATE_FIELDS:
      return {
        ...templateSettings,
        ...payload
      };
    case UPDATE_CREDENTIAL_BODY:
      return {
        ...templateSettings,
        ...payload,
        credentialBody: insertFormChangeIntoArray(
          payload.credentialBody,
          templateSettings.credentialBody
        )
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
