import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { PulseLoader } from 'react-spinners';
import icon from '../../../../images/registrationCongratulation.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const Congratulations = ({ redirector: { redirectToHome } }) => {
  const { t } = useTranslation();
  const { login } = useSession();

  const [loading, setLoading] = useState();

  const handleLogin = async () => {
    setLoading(true);
    await login();
    redirectToHome();
  };

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
          onClick: handleLogin,
          className: 'theme-secondary'
        }}
        buttonText={t('registration.congratulations.login')}
        loading={loading}
        LoadingComponent={LoadingComponent}
      />
    </div>
  );
};

const LoadingComponent = () => <PulseLoader loading size={6} color="#000000" />;

Congratulations.propTypes = {
  redirector: PropTypes.shape({
    redirectToHome: PropTypes.func
  }).isRequired
};

export default withRedirector(Congratulations);
