import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Row } from 'antd';
import { Link } from 'react-router-dom';
import icon from '../../../../images/registrationCongratulation.svg';

const Congratulations = () => {
  const { t } = useTranslation();

  return (
    <Fragment>
      <img src={icon} alt={t('registration.congratulations.alt')} />
      <Row>
        <label>{t('registration.congratulations.title')}</label>
      </Row>
      <Row>
        <label>{t('registration.congratulations.subtitle')}</label>
      </Row>
      <Row>
        <label>{t('registration.congratulations.info')}</label>
      </Row>
      <Row>
        <Link to="/login" className="theme-secondary">
          {t('registration.congratulations.login')}
        </Link>
      </Row>
    </Fragment>
  );
};

export default Congratulations;
