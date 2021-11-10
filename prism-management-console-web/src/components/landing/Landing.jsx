import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { ReactComponent as PreloginLogo } from '../../images/logo-login.svg';
import { ReactComponent as PreloginImg } from '../../images/img-wallet-login.svg';
import { useSession } from '../../hooks/useSession';

import './_style.scss';

const Landing = observer(() => {
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
        <div className="imgLogo">
          <PreloginLogo className="imgLogo" alt={t('landing.logoAlt')} width={200} height={120} />
        </div>
        <div>
          <PreloginImg className="loginImg" alt={t('landing.logoAlt')} width={185} height={120} />
          <div className="WelcomeText">
            <h3>{t('landing.welcome')}</h3>
            <h2>{t('landing.description')}</h2>
          </div>
          <div className="LandingOptions">
            <CustomButton
              buttonProps={{ className: 'theme-secondary', onClick: handleLogin }}
              buttonText={t('landing.login')}
              loading={loading}
            />
          </div>
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
});

export default Landing;
