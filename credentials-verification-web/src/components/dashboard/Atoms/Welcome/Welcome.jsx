import React, { Fragment } from 'react';
import { Col, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import welcomeImage from '../../../../DashboardWelcome.svg';

import './_style.scss';

const Welcome = ({ name, importantInfo }) => {
  const { t } = useTranslation();

  return (
    <div className="WelcomeContainer">
      <div className="WelcomeText">
        <h2>{t('dashboard.welcome.title')}</h2>
      </div>
      <div className="WelcomeImg">
        <img src={welcomeImage} alt={t('dashboard.welcome.image')} />
      </div>
    </div>
  );
};

Welcome.defaultProps = {
  importantInfo: ''
};

Welcome.propTypes = {
  name: PropTypes.string.isRequired,
  importantInfo: PropTypes.string
};

export default Welcome;
