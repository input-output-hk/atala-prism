import React from 'react';
import { Form } from 'antd';
import { useTranslation } from 'react-i18next';
import { UPDATE_FIELDS, useTemplateSettings } from '../../hooks/useTemplateSettings';

const TemplateContext = React.createContext();

const TemplateProvider = props => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [templateSettings, setTemplateSettings] = useTemplateSettings();

  const validateMessages = {
    required: t('credentialTemplateCreation.ertemplateSettingsrors.required')
  };

  const handleFormUpdate = newSetting =>
    setTemplateSettings({ type: UPDATE_FIELDS, payload: newSetting });

  return (
    <Form
      form={form}
      name="control-hooks"
      requiredMark={false}
      layout="vertical"
      validateMessages={validateMessages}
      initialValues={templateSettings}
      onValuesChange={handleFormUpdate}
    >
      <TemplateContext.Provider
        value={{ form, templateSettings, setTemplateSettings }}
        {...props}
      />
    </Form>
  );
};

const useTemplateContext = () => React.useContext(TemplateContext);

const withTemplateProvider = Component => props => (
  <TemplateProvider>
    <Component {...props} />
  </TemplateProvider>
);

export { TemplateProvider, useTemplateContext, withTemplateProvider };
