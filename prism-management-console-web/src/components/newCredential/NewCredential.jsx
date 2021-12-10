import React from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import {
  NEW_CREDENTIALS_STEP_UNIT,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  SELECT_CREDENTIAL_TYPE_STEP,
  CREATE_CREDENTIALS_RESULT
} from '../../helpers/constants';
import TemplateFiltersContainer from '../credentialTemplates/Molecules/Filters/TemplateFiltersContainer';
import { useRedirector } from '../../hooks/useRedirector';
import { useTemplatesByCategoryStore } from '../../hooks/useTemplatesPageStore';

import './_style.scss';

const NewCredential = ({
  isLoading,
  currentStep,
  changeStep,
  renderStep,
  selectedCredentialTypeId,
  hasSelectedRecipients,
  goToCredentialsPreview,
  onCredentialCreation
}) => {
  const { t } = useTranslation();
  const { redirectToCredentials } = useRedirector();

  const { filterSortingProps, templateCategories } = useTemplatesByCategoryStore();

  const goToSelectTargets = () => {
    if (selectedCredentialTypeId) changeStep(SELECT_RECIPIENTS_STEP);
    else message.error(t('newCredential.messages.selectTypeError'));
  };

  const goToDataInput = () => {
    if (hasSelectedRecipients) changeStep(IMPORT_CREDENTIAL_DATA_STEP);
    else message.error(t('newCredential.messages.selectRecipientError'));
  };

  const goToResults = async () => {
    try {
      await onCredentialCreation();
      changeStep(CREATE_CREDENTIALS_RESULT);
    } catch (error) {
      message.error(t('newCredential.messages.selectRecipientError'));
    }
  };

  const goBack = () => changeStep(currentStep - NEW_CREDENTIALS_STEP_UNIT);

  const steps = [
    { key: '0', back: redirectToCredentials, next: goToSelectTargets },
    { key: '1', back: goBack, next: goToDataInput },
    { key: '2', back: goBack, next: goToCredentialsPreview },
    { key: '3', back: goBack, next: goToResults },
    { key: '4', back: goBack, next: redirectToCredentials }
  ];

  const renderStepHeader = () => (
    <div className="StepHeader">
      <WizardTitle
        title={t(`newCredential.title.step${currentStep + 1}`)}
        subtitle={t(`newCredential.subtitle.step${currentStep + 1}`)}
      />
      {currentStep === SELECT_CREDENTIAL_TYPE_STEP && (
        <div className="ActionsHeader EmbeddedTempalteFilters flex">
          <TemplateFiltersContainer
            templateCategories={templateCategories}
            {...filterSortingProps}
          />
        </div>
      )}
    </div>
  );

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
              loading={isLoading}
            />,
            renderStepHeader()
          ]}
        </div>
        <div
          className={
            currentStep !== IMPORT_CREDENTIAL_DATA_STEP
              ? 'WizardContentContainer InfiniteScrollTableContainer'
              : ''
          }
        >
          {renderStep()}
        </div>
      </div>
    </React.Fragment>
  );
};

NewCredential.defaultProps = {
  isLoading: false,
  selectedCredentialTypeId: null
};

NewCredential.propTypes = {
  isLoading: PropTypes.bool,
  currentStep: PropTypes.number.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  selectedCredentialTypeId: PropTypes.string,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  goToCredentialsPreview: PropTypes.func.isRequired,
  onCredentialCreation: PropTypes.func.isRequired
};

export default NewCredential;
