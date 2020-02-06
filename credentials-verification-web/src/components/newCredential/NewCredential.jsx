import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import StepInfo from '../common/Molecules/StepInfo/StepInfo';
import StepFooter from '../common/Molecules/StepFooter/StepFooter';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const NEW_CREDENTIAL_STEP_COUNT = 3;

const SaveDraft = ({ saveDraft }) => {
  const { t } = useTranslation();
  return (
    <CustomButton
      buttonProps={{ onClick: saveDraft, className: 'theme-link' }}
      buttonText={t('actions.saveForLater')}
    />
  );
};

SaveDraft.propTypes = {
  saveDraft: PropTypes.func.isRequired
};

const NewCredential = ({
  currentStep,
  createCredentialTemplate,
  changeStep,
  saveCredential,
  renderStep,
  renderModal,
  openModal,
  selectedGroup
}) => {
  const { t } = useTranslation();
  console.log(currentStep);
  console.log(createCredentialTemplate);
  console.log(changeStep);
  console.log(saveCredential);
  console.log(renderStep);
  console.log(renderModal);
  console.log(openModal);
  console.log(selectedGroup);
  const steps = [
    { stepTitle: 'newCredential.steps.step1' },
    { stepTitle: 'newCredential.steps.step2' },
    { stepTitle: 'newCredential.steps.step3' }
  ];

  const next = [
    createCredentialTemplate,
    () => {
      if (!selectedGroup) {
        message.error(t('newCredential.form.errors.noGroupSelected'));
        return;
      }

      changeStep(currentStep + 1);
    },
    null
  ];

  const goBack = () => changeStep(currentStep - 1);
  const previous = [null, goBack, goBack];

  return (
    <React.Fragment>
      {renderModal()}
      <div className="CredentialMainContent">
        <div className="CredentialContainerTop">
          <StepInfo
            title="newCredential.title"
            subtitle="newCredential.subtitle"
            currentStep={currentStep}
            steps={steps}
          />
          {renderStep()}
        </div>
        <StepFooter
          currentStep={currentStep}
          stepCount={NEW_CREDENTIAL_STEP_COUNT}
          previousStep={previous[currentStep]}
          nextStep={next[currentStep]}
          finish={saveCredential}
          renderExtraOptions={() => <SaveDraft saveDraft={openModal} />}
          finishText="newCredential.save"
        />
      </div>
    </React.Fragment>
  );
};

NewCredential.defaultProps = {
  selectedGroup: { name: '' }
};

NewCredential.propTypes = {
  currentStep: PropTypes.number.isRequired,
  saveCredential: PropTypes.func.isRequired,
  createCredentialTemplate: PropTypes.func.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  renderModal: PropTypes.func.isRequired,
  openModal: PropTypes.func.isRequired,
  selectedGroup: PropTypes.shape({ name: PropTypes.string })
};

export default NewCredential;
