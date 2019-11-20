import React from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';
import icon from '../../../../images/registrationCongratulation.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const Congratulations = () => {
  const { t } = useTranslation();
  const history = useHistory();

  return (
    <div className="Congratulations">
      <div className="CongratulationsContainer">
        <img src={icon} alt={t('registration.congratulations.alt')} />
        <h2>
          <strong>{t('registration.congratulations.title')}</strong>
        </h2>
        <h2>{t('registration.congratulations.subtitle')}</h2>
        <p>{t('registration.congratulations.info')}</p>
      </div>
      <CustomButton
        buttonProps={{
          onClick: () => history.push('/login'),
          className: 'theme-secondary'
        }}
        buttonText={t('registration.congratulations.login')}
      />
    </div>
  );
};

export default Congratulations;
