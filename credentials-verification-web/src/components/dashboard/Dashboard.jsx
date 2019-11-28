import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import Welcome from './Atoms/Welcome/Welcome';
import CurrentBundle from './Atoms/CurrentBundle/CurrentBundle';
import ConnectionSummary from './Molecules/ConnectionSummary/ConnectionSummary';
import TransactionSummary from './Molecules/TransactionSummary/TransactionSummary';
import { getBrowserLanguage } from '../../helpers/languageUtils';
import { ISSUER } from '../../helpers/constants';

import './_style.scss';

const Dashboard = ({ name, bundle, credentials, proofRequests }) => {
  const { t } = useTranslation();

  moment.locale(getBrowserLanguage());

  return (
    <div className="DashboardContainer Wrapper">
      <div className="DashboardHeader">
        <h1>{t('dashboard.title')}</h1>
        <p>{moment().format('DD/MM/YYYY')}</p>
      </div>
      <div className="DashboardContent">
        <Welcome name={name} importantInfo={t('dashboard.welcome.subtitle')} />
        {false && <CurrentBundle bundle={bundle} />}
      </div>
      <div className="DashboardContentBottom">
        <ConnectionSummary weekAmount={0} monthAmount={0} yearAmount={0} />
        {localStorage.getItem('userRole') === ISSUER && (
          <TransactionSummary credentials={credentials} proofRequests={proofRequests} />
        )}
      </div>
    </div>
  );
};

Dashboard.defaultProps = {
  bundle: undefined,
  name: 'John'
};

Dashboard.propTypes = {
  name: PropTypes.string,
  bundle: PropTypes.shape({
    remaining: PropTypes.number,
    totalConnections: PropTypes.number
  }),
  credentials: PropTypes.shape({
    week: PropTypes.number,
    month: PropTypes.number
  }).isRequired,
  proofRequests: PropTypes.shape({
    week: PropTypes.number,
    month: PropTypes.number
  }).isRequired
};

export default Dashboard;
