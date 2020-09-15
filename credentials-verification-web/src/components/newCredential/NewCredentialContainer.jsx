import React, { useState } from 'react';
import PropTypes from 'prop-types';
import NewCredential from './NewCredential';
import { withApi } from '../providers/withApi';
import TypeSelection from './Organism/TypeSelection/TypeSelection';
import { withRedirector } from '../providers/withRedirector';

const NewCredentialContainer = ({ api }) => {
  const [currentStep, setCurrentStep] = useState(0);

  const [credentialType, setCredentialType] = useState();

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return (
          <TypeSelection
            credentialTypes={credentialTypes}
            onTypeSelection={setCredentialType}
            selectedType={credentialType}
          />
        );
      case 1:
        return null; // TODO: Implement recipients selection
      case 2:
        return null; // TODO: Implement credential information import
      default:
        return null; // TODO: Implement credential visualisation + signing
    }
  };

  return (
    <NewCredential
      currentStep={currentStep}
      changeStep={setCurrentStep}
      renderStep={renderStep}
      credentialType={credentialType}
    />
  );
};

NewCredentialContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func,
      createCredential: PropTypes.func
    }).isRequired,
    getIndividuals: PropTypes.func,
    wallet: PropTypes.shape({ isIssuer: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
