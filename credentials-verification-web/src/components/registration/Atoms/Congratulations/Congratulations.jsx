import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationCongratulation.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';
import { withRedirector } from '../../../providers/withRedirector';

const Congratulations = ({ redirector: { redirectToLogin } }) => {
  const { t } = useTranslation();

  return (
    <div className="Congratulations Wrapper">
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
          onClick: redirectToLogin,
          className: 'theme-secondary'
        }}
        buttonText={t('registration.congratulations.login')}
      />
    </div>
  );
};

Congratulations.propTypes = {
  redirector: PropTypes.shape({
    redirectToLogin: PropTypes.func
  }).isRequired
};

export default withRedirector(Congratulations);
