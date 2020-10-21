import React from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../providers/withRedirector';
import StepInfo from '../common/Molecules/StepInfo/StepInfo';
import GenericFooter from '../common/Molecules/GenericFooter/GenericFooter';
import {
  NEW_CREDENTIALS_STEP_UNIT,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  NEW_CREDENTIAL_STEP_COUNT
} from '../../helpers/constants';
import './_style.scss';

const NewCredential = ({
  currentStep,
  changeStep,
  saveCredential,
  renderStep,
  credentialType,
  hasSelectedRecipients,
  goToCredentialsPreview,
  redirector: { redirectToCredentials }
}) => {
  const { t } = useTranslation();

  const steps = [
    { stepTitle: 'newCredential.steps.step1' },
    { stepTitle: 'newCredential.steps.step2' },
    { stepTitle: 'newCredential.steps.step3' },
    { stepTitle: 'newCredential.steps.step4' }
  ];

  const goToSelectTargets = () => {
    if (credentialType) changeStep(SELECT_RECIPIENTS_STEP);
    else message.error(t('newCredential.messages.selectTypeError'));
  };

  const goToDataInput = () => {
    if (hasSelectedRecipients) changeStep(IMPORT_CREDENTIAL_DATA_STEP);
    else message.error(t('newCredential.messages.selectRecipientError'));
  };

  const onCredentialsCreationFinish = () => {
    // TODO
  };

  const goBack = () => changeStep(currentStep - NEW_CREDENTIALS_STEP_UNIT);

  const next = [
    goToSelectTargets,
    goToDataInput,
    goToCredentialsPreview,
    onCredentialsCreationFinish
  ];

  const previous = [redirectToCredentials, goBack, goBack, goBack];

  return (
    <React.Fragment>
      <div className="CredentialMainContent">
        <div className="CredentialContainerTop">
          <StepInfo title="newCredential.title" currentStep={currentStep} steps={steps} />
        </div>
        {renderStep()}
        {currentStep !== IMPORT_CREDENTIAL_DATA_STEP && (
          <GenericFooter
            currentStep={currentStep}
            stepCount={NEW_CREDENTIAL_STEP_COUNT}
            previous={previous[currentStep]}
            next={next[currentStep]}
            finish={saveCredential}
            finishText="newCredential.save"
          />
        )}
      </div>
    </React.Fragment>
  );
};

NewCredential.propTypes = {
  currentStep: PropTypes.number.isRequired,
  saveCredential: PropTypes.func.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  credentialType: PropTypes.string.isRequired,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  redirector: PropTypes.shape({ redirectToCredentials: PropTypes.func }).isRequired,
  goToCredentialsPreview: PropTypes.func.isRequired
};

export default withRedirector(NewCredential);
