import React, { useEffect, useState } from 'react';
import { Form } from 'antd';
import { useTranslation } from 'react-i18next';
import Logger from '../../helpers/Logger';
import {
  UPDATE_CREDENTIAL_BODY,
  UPDATE_FIELDS,
  useTemplateSettings
} from '../../hooks/useTemplateSettings';

const TemplateSketchContext = React.createContext();

const TemplateSketchProvider = props => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [templateSettings, setTemplateSettings] = useTemplateSettings();
  const [templatePreview, setTemplatePreview] = useState();

  const validateMessages = {
    required: t('credentialTemplateCreation.errors.required')
  };

  const handleFormUpdate = newSetting => {
    if (newSetting.credentialBody) {
      setTemplateSettings({ type: UPDATE_CREDENTIAL_BODY, payload: newSetting });
    } else {
      setTemplateSettings({ type: UPDATE_FIELDS, payload: newSetting });
    }
  };

  useEffect(() => {
    Logger.debug('[Template Creation] partial template settings:', templateSettings);
  }, [templateSettings]);

  return (
    <Form
      form={form}
      name="template-form"
      requiredMark={false}
      layout="vertical"
      validateMessages={validateMessages}
      initialValues={templateSettings}
      onValuesChange={handleFormUpdate}
    >
      <TemplateSketchContext.Provider
        value={{ form, templateSettings, setTemplateSettings, templatePreview, setTemplatePreview }}
        {...props}
      />
    </Form>
  );
};

const useTemplateSketchContext = () => React.useContext(TemplateSketchContext);

const withTemplateSketchProvider = Component => props => (
  <TemplateSketchProvider>
    <Component {...props} />
  </TemplateSketchProvider>
);

export { TemplateSketchProvider, useTemplateSketchContext, withTemplateSketchProvider };
