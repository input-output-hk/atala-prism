import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Row } from 'antd';
import { Link } from 'react-router-dom';
import icon from '../../../../images/registrationCongratulation.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const Congratulations = () => {
  const { t } = useTranslation();

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
          className: 'theme-secondary'
        }}
        buttonText={t('registration.congratulations.login')}
      />
      <Link to="/login">{t('registration.congratulations.login')}</Link>
    </div>
  );
};

export default Congratulations;
