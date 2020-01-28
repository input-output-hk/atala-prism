import React, { Fragment } from 'react';
import { Col, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import welcomeImage from '../../../../images/DashboardWelcome.svg';

import './_style.scss';

const Welcome = ({ name, importantInfo }) => {
  const { t } = useTranslation();

  return (
    // Add class IssuerUser or VerifierUser to change Theme Color
    <div className="WelcomeContainer IssuerUser">
      <div className="WelcomeText">
        {/* Add class IssuerUser or VerifierUser to change Theme Color */}
        <h2 className="IssuerUser">{t('dashboard.welcome.title')}</h2>
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
