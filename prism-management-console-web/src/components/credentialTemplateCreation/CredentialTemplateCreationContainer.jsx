import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { Form } from 'antd';
import { SELECT_TEMPLATE_CATEGORY } from '../../helpers/constants';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCreationStep from './Organisms/TemplateCreationStep';
import { useTemplateStore } from '../../hooks/useTemplateStore';
import { defaultTemplateSketch } from '../../helpers/templateHelpers';
import { useTemplateSketch } from '../../hooks/useTemplateSketch';

const CredentialTemplateCreationContainer = observer(() => {
  const { t } = useTranslation();

  useTemplateStore({ fetch: true });

  const { setForm, setSketchState: handleValuesUpdate } = useTemplateSketch({ reset: true });

  const [form] = Form.useForm();
  setForm(form);

  const validateMessages = {
    required: t('credentialTemplateCreation.errors.required')
  };

  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);
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
