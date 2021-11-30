import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { Form } from 'antd';
import { TEMPLATE_NAME_ICON_CATEGORY } from '../../helpers/constants';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCreationStep from './Organisms/TemplateCreationStep';
import { useTemplatePageStore } from '../../hooks/useTemplatesPageStore';
import { defaultTemplateSketch } from '../../helpers/templateHelpers';
import { useTemplateSketch } from '../../hooks/useTemplateSketch';

const CredentialTemplateCreationContainer = observer(() => {
  const { t } = useTranslation();

  // TODO: replace with own feature store for tempalte creation
  const { initTemplateStore } = useTemplatePageStore();

  useEffect(() => {
    initTemplateStore();
  }, [initTemplateStore]);

  const { setForm, setSketchState: handleValuesUpdate } = useTemplateSketch({ reset: true });

  const [currentStep, setCurrentStep] = useState(TEMPLATE_NAME_ICON_CATEGORY);
  const [form] = Form.useForm();
  setForm(form);

  const validateMessages = {
    required: t('credentialTemplateCreation.errors.required')
  };

  return (
    <div className="TemplateMainContent">
      <Form
        form={form}
        name="template-form"
        requiredMark={false}
        layout="vertical"
        validateMessages={validateMessages}
        initialValues={defaultTemplateSketch}
        onValuesChange={handleValuesUpdate}
      >
        <CredentialTemplateCreation currentStep={currentStep} changeStep={setCurrentStep} />
        <TemplateCreationStep currentStep={currentStep} />
      </Form>
    </div>
  );
});

export default CredentialTemplateCreationContainer;
