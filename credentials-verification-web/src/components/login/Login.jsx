import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import LoginForm from './Organisms/Form/LoginForm';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { refShape } from '../../helpers/propShapes';
import logo from '../../images/loginLogo.svg';

import './_style.scss';

const Login = ({ formRef, handleLogin }) => {
  const { t } = useTranslation();

  return (
    <div className="LoginContainer">
      <div className="LoginCard">
        <img src={logo} alt={t('login.logoAlt')} />
        <div className="WelcomeText">
          <h3>{t('login.welcome.title')}</h3>
          <h3>{t('login.welcome.subtitle')}</h3>
        </div>
        <div className="FormContainer">
          <LoginForm ref={formRef} />
        </div>
        <div className="ForgotPassword">
          <p>{t('login.forgotPassword')}</p>
          <Link to="forgotPassword">{t('login.goToForgotPassword')}</Link>
        </div>
        <CustomButton
          buttonProps={{ className: 'theme-secondary', onClick: handleLogin }}
          buttonText={t('login.submit')}
        />
      </div>
    </div>
  );
};

Login.propTypes = {
  formRef: refShape.isRequired,
  handleLogin: PropTypes.isRequired
};

export default Login;
