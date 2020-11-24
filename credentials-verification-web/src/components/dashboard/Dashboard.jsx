import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { pick } from 'lodash';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import Welcome from './Atoms/Welcome/Welcome';
import CurrentBundle from './Atoms/CurrentBundle/CurrentBundle';
import DashboardCard from './organism/DashboardCard';
import DashboardCardGroup from './organism/DashboardCardGroup';
import DashboardCardCredential from './organism/DashboardCardCredential';
import Logger from '../../helpers/Logger';
import './_style.scss';
import { useTranslationWithPrefix } from '../../hooks/useTranslationWithPrefix';
import { longDateFormatter } from '../../helpers/formatters';

const Dashboard = ({ api, name, bundle }) => {
  const { t } = useTranslation();
  const tp = useTranslationWithPrefix('dashboard');
  const [contactsStats, setContactsStats] = useState({});
  const [groupsStats, setGroupsStats] = useState({});
  const [credentialsStats, setCredentialsStats] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getStatistics();
  }, []);

  const getStatistics = async () => {
    setLoading(true);
    try {
      const statistics = await api.summaryManager.getStatistics();
      setContactsStats(statistics.contacts);
      setGroupsStats(statistics.groups);
      setCredentialsStats(statistics.credentials);
    } catch (error) {
      Logger.error('Error getting statistics: ', error);
      message.error(t('errors.errorGetting', { model: 'statistics' }));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="DashboardContainer Wrapper">
      <div className="DashboardHeader">
        <h1>{tp('title')}</h1>
        <p>{longDateFormatter()}</p>
      </div>
      <div className="DashboardContent">
        <Welcome name={name} importantInfo={tp('welcome.subtitle')} />
        {false && <CurrentBundle bundle={bundle} />}
      </div>
      <div className="DashboardContentBottom">
        <h1>{tp('titleBottom')}</h1>
        <div className="dashboardCardContainer">
          <DashboardCard data={contactsStats} loading={loading} />
          <DashboardCardGroup data={groupsStats} loading={loading} />
          <DashboardCardCredential data={credentialsStats} loading={loading} />
        </div>
      </div>
    </div>
  );
};

Dashboard.defaultProps = {
  bundle: undefined,
  name: 'Username'
};

Dashboard.propTypes = {
  api: PropTypes.shape({
    summaryManager: PropTypes.shape({
      getStatistics: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  name: PropTypes.string,
  bundle: PropTypes.shape({
    remaining: PropTypes.number,
    totalConnections: PropTypes.number
  })
};

export default withApi(Dashboard);
