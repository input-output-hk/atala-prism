import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { message } from 'antd';
import Welcome from './Atoms/Welcome/Welcome';
import DashboardCardGroup from './organism/DashboardCardGroup';
import DashboardCardCredential from './organism/DashboardCardCredential';
import Logger from '../../helpers/Logger';
import { useTranslationWithPrefix } from '../../hooks/useTranslationWithPrefix';
import { longDateFormatter } from '../../helpers/formatters';
import {
  CONFIRMED,
  LOADING,
  UNCONFIRMED,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../../helpers/constants';
import WaitBanner from './Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { useApi } from '../../hooks/useApi';
import DashboardCard from './organism/DashboardCard';

import './_style.scss';

const Dashboard = observer(({ name }) => {
  const { t } = useTranslation();
  const tp = useTranslationWithPrefix('dashboard');
  const { summaryManager } = useApi();

  const [contactsStats, setContactsStats] = useState();
  const [groupsStats, setGroupsStats] = useState();
  const [credentialsStats, setCredentialsStats] = useState();
  const [loading, setLoading] = useState(false);

  const {
    showUnconfirmedAccountError,
    removeUnconfirmedAccountError,
    accountStatus
  } = useSession();

  useEffect(() => {
    const getStatistics = async () => {
      setLoading(true);
      try {
        const statistics = await summaryManager.getStatistics();
        setContactsStats(statistics.contacts);
        setGroupsStats(statistics.groups);
        setCredentialsStats(statistics.credentials);
        removeUnconfirmedAccountError();
      } catch (error) {
        Logger.error('Error getting statistics: ', error);
        if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
          showUnconfirmedAccountError();
        } else {
          removeUnconfirmedAccountError();
          message.error(t('errors.errorGetting', { model: 'statistics' }));
        }
      } finally {
        setLoading(false);
      }
    };

    getStatistics();
  }, [summaryManager, removeUnconfirmedAccountError, showUnconfirmedAccountError, t]);

  return (
    <div className="DashboardContainer Wrapper">
      <div className="DashboardHeader">
        <h1>{tp('title')}</h1>
        <p>{longDateFormatter()}</p>
      </div>
      <div className="DashboardContent">
        {accountStatus === LOADING && <SimpleLoading size="md" />}
        {accountStatus === CONFIRMED && (
          <Welcome name={name} importantInfo={tp('welcome.subtitle')} />
        )}
        {accountStatus === UNCONFIRMED && <WaitBanner />}
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
});

Dashboard.defaultProps = {
  name: 'Username'
};

Dashboard.propTypes = {
  name: PropTypes.string
};

export default Dashboard;
