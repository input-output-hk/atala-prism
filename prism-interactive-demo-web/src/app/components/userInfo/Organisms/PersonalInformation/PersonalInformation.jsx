import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import CustomButton from '../../../../../components/customButton/CustomButton';
import PersonalInformationForm from '../PersonalInformationForm/PersonalInformationForm';

import './_style.scss';
import AlertInfo from '../../../common/Atoms/AlertInfo/AlertInfo';
import PersonalInfoItems from '../../Molecules/PersonalInfoItems/PersonalInfoItems';

const PersonalInformation = ({ nextStep, personalInfoRef }) => {
  const { t } = useTranslation();

  return (
    <div className="PersonalInformation">
      <h1>{t('credential.personalInformation.title')}</h1>
      <h2>{t('credential.personalInformation.explanation')}</h2>
      <PersonalInformationForm ref={personalInfoRef} />
      <PersonalInfoItems />
      <AlertInfo
        titleAlert={t('credential.personalInformation.titleAlert')}
        textAlert={t('credential.personalInformation.textAlert')}
      />
      <CustomButton
        buttonProps={{
          onClick: nextStep,
          className: 'theme-primary',
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
