import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { SELECT_TEMPLATE_CATEGORY } from '../../helpers/constants';
import { credentialTypesManagerShape } from '../../helpers/propShapes';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import { withTemplateProvider } from '../providers/TemplateContext';
import TemplateCreationStep from './Organisms/TemplateCreationStep';

const CredentialTemplateCreationContainer = ({ api: { credentialTypesManager } }) => {
  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);
  return (
    <div className="TemplateMainContent">
      <CredentialTemplateCreation currentStep={currentStep} changeStep={setCurrentStep} />
      <TemplateCreationStep
        currentStep={currentStep}
        credentialTypesManager={credentialTypesManager}
      />
    </div>
  );
};

CredentialTemplateCreationContainer.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape
  }).isRequired
};

export default withTemplateProvider(CredentialTemplateCreationContainer);
