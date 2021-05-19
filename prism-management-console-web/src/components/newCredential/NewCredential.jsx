import React from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../providers/withRedirector';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import {
  NEW_CREDENTIALS_STEP_UNIT,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP
} from '../../helpers/constants';

import './_style.scss';

const NewCredential = ({
  isLoading,
  currentStep,
  changeStep,
  renderStep,
  credentialType,
  hasSelectedRecipients,
  goToCredentialsPreview,
  onSuccess,
  redirector: { redirectToCredentials }
}) => {
  const { t } = useTranslation();

  const goToSelectTargets = () => {
    if (credentialType) changeStep(SELECT_RECIPIENTS_STEP);
    else message.error(t('newCredential.messages.selectTypeError'));
  };

  const goToDataInput = () => {
    if (hasSelectedRecipients) changeStep(IMPORT_CREDENTIAL_DATA_STEP);
    else message.error(t('newCredential.messages.selectRecipientError'));
  };

  const goBack = () => changeStep(currentStep - NEW_CREDENTIALS_STEP_UNIT);

  const steps = [
    { back: redirectToCredentials, next: goToSelectTargets },
    { back: goBack, next: goToDataInput },
    { back: goBack, next: goToCredentialsPreview },
    { back: goBack, next: onSuccess }
  ];

  const isLastStep = currentStep + 1 === steps.length;

  return (
    <React.Fragment>
      <div className="CredentialMainContent">
        <div className="TitleContainer">
          {currentStep !== IMPORT_CREDENTIAL_DATA_STEP && [
            <GenericStepsButtons
              steps={steps}
              currentStep={currentStep}
              disableBack={isLoading}
              disableNext={isLoading}
              loading={isLoading && isLastStep}
            />,
            <WizardTitle
              title={t(`newCredential.title.step${currentStep + 1}`)}
              subtitle={t(`newCredential.subtitle.step${currentStep + 1}`)}
            />
          ]}
        </div>
        <div
          className={currentStep !== IMPORT_CREDENTIAL_DATA_STEP ? 'WizardContentContainer' : ''}
        >
          {renderStep()}
        </div>
      </div>
    </React.Fragment>
  );
};

NewCredential.defaultProps = {
  isLoading: false
};

NewCredential.propTypes = {
  isLoading: PropTypes.bool,
  currentStep: PropTypes.number.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  credentialType: PropTypes.string.isRequired,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  redirector: PropTypes.shape({ redirectToCredentials: PropTypes.func }).isRequired,
  goToCredentialsPreview: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired
};

export default withRedirector(NewCredential);
