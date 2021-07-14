import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DESIGN_TEMPLATE,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { useCredentialTypes, useTemplateCategories } from '../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../helpers/propShapes';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import DesignTemplateStep from './Organisms/DesignTemplateStep/DesignTemplateStep';
import { useTemplateContext, withTemplateProvider } from '../providers/TemplateContext';
import TemplateCategorySelectionStep from './Organisms/CategoryStep/TemplateCategorySelector';
import SuccessBanner from '../common/Molecules/SuccessPage/SuccessBanner';
import { withRedirector } from '../providers/withRedirector';

const CredentialTemplateCreationContainer = ({
  api: { credentialTypesManager },
  redirector: { redirectToCredentialTemplates }
}) => {
  const { t } = useTranslation();
  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);
  const { templateSettings } = useTemplateContext();

  const { credentialTypes } = useCredentialTypes(credentialTypesManager);
  const { templateCategories } = useTemplateCategories(credentialTypesManager);

  const renderStep = () => {
    switch (currentStep) {
      default:
      case SELECT_TEMPLATE_CATEGORY:
        return (
          <TemplateCategorySelectionStep
            templateCategories={templateCategories}
            existingTemplates={credentialTypes}
          />
        );
      case DESIGN_TEMPLATE:
        return <DesignTemplateStep templateSettings={templateSettings} />;
      case TEMPLATE_CREATION_RESULT: {
        return (
          <SuccessBanner
            title={t('credentialTemplateCreation.step3.successTitle')}
            message={t('credentialTemplateCreation.step3.successMessage')}
            buttonText={t('credentialTemplateCreation.step3.continueButton')}
            onContinue={redirectToCredentialTemplates}
          />
        );
      }
    }
  };

  return (
    <CredentialTemplateCreation
      currentStep={currentStep}
      changeStep={setCurrentStep}
      renderStep={renderStep}
    />
  );
};

CredentialTemplateCreationContainer.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentialTemplates: PropTypes.func.isRequired
  }).isRequired
};

export default withTemplateProvider(withRedirector(CredentialTemplateCreationContainer));
