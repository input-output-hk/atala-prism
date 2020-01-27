import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import preloginLogo from '../../images/atala-logo-loading.svg';

import './_style.scss';
import { withRedirector } from '../providers/withRedirector';
import LanguageSelector from '../common/Molecules/LanguageSelector/LanguageSelector';

const Landing = ({ redirector: { redirectToLogin, redirectToRegistration } }) => {
  const { t } = useTranslation();

  return (
    <div className="LandingContainer">
      <div className="LangSelector">
        <LanguageSelector />
      </div>
      <div className="LandingCard">
        <img src={preloginLogo} alt={t('landing.logoAlt')} />
        <div className="WelcomeText">
          <h3>{t('landing.welcome')}</h3>
        </div>
        <div className="LandingOptions">
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              onClick: redirectToRegistration
            }}
            buttonText={t('landing.register')}
          />
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: redirectToLogin }}
            buttonText={t('landing.login')}
          />
        </div>
      </div>
    </div>
  );
};

Landing.propTypes = {
  redirector: PropTypes.shape({
    redirectToLogin: PropTypes.func
  }).isRequired
};

export default withRedirector(Landing);
