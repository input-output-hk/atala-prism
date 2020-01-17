import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import PersonalInformationForm from '../PersonalInformationForm/PersonalInformationForm';

import './_style.scss';

const PersonalInformation = ({ nextStep, personalInfoRef }) => {
  const { t } = useTranslation();

  return (
    <div className="PersonalInformation">
      <h1>{t('credential.personalInformation.title')}</h1>
      <h2>{t('credential.personalInformation.explanation')}</h2>
      <PersonalInformationForm ref={personalInfoRef} />
      <CustomButton
        buttonProps={{
          onClick: nextStep,
          className: 'theme-secondary',
          disabled: false
        }}
        buttonText={t('credential.personalInformation.askForCredential')}
      />
    </div>
  );
};

PersonalInformation.propTypes = {
  nextStep: PropTypes.func.isRequired,
  personalInfoRef: PropTypes.shape.isRequired
};

export default PersonalInformation;
