import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import PropTypes from 'prop-types';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import preloginLogo from '../../images/logo-login.svg';
import preloginImg from '../../images/img-wallet-login.svg';
import { withRedirector } from '../providers/withRedirector';
import LanguageSelector from '../common/Molecules/LanguageSelector/LanguageSelector';
import { useSession } from '../providers/SessionContext';

import './_style.scss';

const Landing = ({ redirector: { redirectToRegistration } }) => {
  const { t } = useTranslation();
  const { login } = useSession();

  const [loading, setLoading] = useState();

  const handleLogin = async () => {
    setLoading(true);
    try {
      await login();
      setLoading(false);
    } catch (error) {
      setLoading(false);
    }
  };

  return (
    <div className="LandingContainer">
      <div className="LandingCard">
        <img className="imgLogo" src={preloginLogo} alt={t('landing.logoAlt')} />
        <img className="loginImg" src={preloginImg} alt={t('landing.logoAlt')} />
        <div className="WelcomeText">
          <h3>{t('landing.welcome')}</h3>
          <h2>{t('landing.description')}</h2>
        </div>
        <div className="LandingOptions">
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: handleLogin }}
            buttonText={t('landing.login')}
            loading={loading}
            LoadingComponent={LoadingComponent}
          />
        </div>
        <div className="browserWalletOption">
          <p>
            {t('landing.getWallet')}
            <a href="/instructions">{t('landing.clickHere')}</a>
          </p>
        </div>
      </div>
    </div>
  );
};

const LoadingComponent = () => <PulseLoader loading size={6} color="#000000" />;

Landing.propTypes = {
  redirector: PropTypes.shape({
    redirectToRegistration: PropTypes.func
  }).isRequired
};

export default withRedirector(Landing);
