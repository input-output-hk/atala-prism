import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Form } from 'antd';
import { useTranslation } from 'react-i18next';
import {
  DESIGN_TEMPLATE,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { useCredentialTypes, useTemplateCategories } from '../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../helpers/propShapes';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCategorySelectionStep from './Organisms/TemplateCategorySelectionStep/TemplateCategorySelectionStep';

const CredentialTemplateCreationContainer = ({ api: { credentialTypesManager } }) => {
  const { t } = useTranslation();
  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);

  const [form] = Form.useForm();

  const { credentialTypes } = useCredentialTypes(credentialTypesManager);
  const { templateCategories } = useTemplateCategories(credentialTypesManager);

  const validateMessages = {
    required: t('credentialTemplateCreation.errors.required')
  };

  const renderStep = () => {
    switch (currentStep) {
      default:
      case SELECT_TEMPLATE_CATEGORY:
        return (
          <TemplateCategorySelectionStep
            templateCategories={templateCategories}
            existingTemplates={credentialTypes}
            form={form}
          />
        );
      case DESIGN_TEMPLATE:
        return (
          <div>
            {/* FIXME: add design template component */}
            (Design Template Component)
          </div>
        );
      case TEMPLATE_CREATION_RESULT: {
        return (
          <div>
            {/* FIXME: add template creation result component */}
            (Template Creation Result Component)
          </div>
        );
      }
    }
  };

  return (
    <Form
      form={form}
      name="control-hooks"
      requiredMark={false}
      layout="vertical"
      validateMessages={validateMessages}
    >
      <CredentialTemplateCreation
        currentStep={currentStep}
        changeStep={setCurrentStep}
        renderStep={renderStep}
        form={form}
      />
    </Form>
  );
};

CredentialTemplateCreationContainer.propTypes = {
  api: PropTypes.shape({ credentialTypesManager: credentialTypesManagerShape }).isRequired
};

export default CredentialTemplateCreationContainer;
