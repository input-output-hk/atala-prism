import React from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import StepInfo from '../common/Molecules/StepInfo/StepInfo';
import StepFooter from '../common/Molecules/StepFooter/StepFooter';

import './_style.scss';

const NEW_CREDENTIAL_STEP_COUNT = 3;

const NewCredential = ({ currentStep, changeStep, saveCredential, renderStep, credentialType }) => {
  const steps = [
    { stepTitle: 'newCredential.steps.step1' },
    { stepTitle: 'newCredential.steps.step2' },
    { stepTitle: 'newCredential.steps.step3' }
  ];

  const next = [
    () => {
      if (credentialType) message.warn('Further steps are not implemented yet');
      else message.error('Please select a credential type');
    },
    null,
    null,
    null
  ];

  const goBack = () => changeStep(currentStep - 1);
  const previous = [null, goBack, goBack, goBack];

  return (
    <React.Fragment>
      <div className="CredentialMainContent">
        <div className="CredentialContainerTop">
          <StepInfo title="newCredential.title" currentStep={currentStep} steps={steps} />
          {renderStep()}
        </div>
        <StepFooter
          currentStep={currentStep}
          stepCount={NEW_CREDENTIAL_STEP_COUNT}
          previousStep={previous[currentStep]}
          nextStep={next[currentStep]}
          finish={saveCredential}
          finishText="newCredential.save"
        />
      </div>
    </React.Fragment>
  );
};

NewCredential.propTypes = {
  currentStep: PropTypes.number.isRequired,
  saveCredential: PropTypes.func.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  credentialType: PropTypes.string.isRequired
};

export default NewCredential;
