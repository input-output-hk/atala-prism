import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  DESIGN_TEMPLATE,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { useCredentialTypes, useTemplateCategories } from '../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../helpers/propShapes';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCategorySelectionStep from './Organisms/TemplateCategorySelectionStep/TemplateCategorySelectionStep';
import DesignTemplateStep from './Organisms/DesignTemplateStep/DesignTemplateStep';
import { useTemplateContext, withTemplateProvider } from '../providers/TemplateContext';

const CredentialTemplateCreationContainer = ({ api: { credentialTypesManager } }) => {
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
          <div>
            {/* FIXME: add template creation result component */}
            (Template Creation Result Component)
          </div>
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
  api: PropTypes.shape({ credentialTypesManager: credentialTypesManagerShape }).isRequired
};

export default withTemplateProvider(CredentialTemplateCreationContainer);
