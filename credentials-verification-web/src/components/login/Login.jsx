import React, { createRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Col, Card, Row } from 'antd';
import LoginForm from './Organisms/Form/LoginForm';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import logo from '../../images/loginLogo.svg';
import background from '../../images/loginBackground.svg';

const Login = ({ formRef, handleLogin }) => {
  const { t } = useTranslation();

  const rowProps = { align: 'middle', gutter: '32', type: 'flex' };

  return (
    <div className="login">
      <Card>
        <Row {...rowProps}>
          <img src={logo} alt={t('login.logoAlt')} />
        </Row>
        <Row {...rowProps}>
          <label>{t('login.welcome.title')}</label>
        </Row>
        <Row {...rowProps}>
          <label>{t('login.welcome.subtitle')}</label>
        </Row>
        <Row {...rowProps}>
          <LoginForm ref={formRef} />
        </Row>
        <Row {...rowProps}>
          <label>{t('login.forgotPassword')}</label>
          <Link to="forgotPassword">{t('login.goToForgotPassword')}</Link>
        </Row>
        <Row {...rowProps}>
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: handleLogin }}
            buttonText={t('login.submit')}
          />
        </Row>
      </Card>
    </div>
  );
};

export default Login;
