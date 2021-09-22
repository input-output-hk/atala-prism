import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import Welcome from './Atoms/Welcome/Welcome';
import CurrentBundle from './Atoms/CurrentBundle/CurrentBundle';
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
import { useSession } from '../providers/SessionContext';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import TutorialModal from '../tutorial/tutorialModal';
import TutorialTool from '../tutorial/tutorialTool/tutorialTool';
import TutorialPopover from '../tutorial/tutorialTool/tutorialPopover';

import './_style.scss';

const Dashboard = ({ api, name, bundle }) => {
  const { t } = useTranslation();
  const tp = useTranslationWithPrefix('dashboard');

  const {
    configuration: { tutorialProgress: storedTutorialProgress, saveTutorialProgress }
  } = api;

  const [tutorialProgress, setTutorialProgress] = useState(storedTutorialProgress);
  const [, setContactsStats] = useState();
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
        const statistics = await api.summaryManager.getStatistics();
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
  }, [api.summaryManager, removeUnconfirmedAccountError, showUnconfirmedAccountError, t]);

  useEffect(() => {
    saveTutorialProgress(tutorialProgress);
  }, [tutorialProgress, saveTutorialProgress]);

  const updateTutorialProgress = useCallback(
    update => {
      const newTutorialProgress = Object.assign({}, tutorialProgress, update);
      setTutorialProgress(newTutorialProgress);
    },
    [tutorialProgress]
  );

  const onStartTutorial = () => updateTutorialProgress({ started: true });
  const onPauseTutorial = () => updateTutorialProgress({ paused: true });

  const tutorialIsRunning =
    tutorialProgress.started && !tutorialProgress.paused && !tutorialProgress.finished;

  return (
    <div className="DashboardContainer Wrapper">
      <div className="DashboardHeader">
        <h1>{tp('title')}</h1>
        <p>{longDateFormatter()}</p>
        <TutorialModal
          isOpen={!tutorialProgress.started && !tutorialProgress.paused}
          onStart={onStartTutorial}
          onPause={onPauseTutorial}
        />
      </div>
      <div className="DashboardContent">
        {accountStatus === LOADING && <SimpleLoading size="md" />}
        {accountStatus === CONFIRMED && (
          <Welcome name={name} importantInfo={tp('welcome.subtitle')} />
        )}
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        {false && <CurrentBundle bundle={bundle} />}
      </div>
      <div className="DashboardContentBottom">
        <h1>{tp('titleBottom')}</h1>
        <div className="dashboardCardContainer">
          <TutorialPopover currentStep={tutorialProgress.basicSteps} />
          <DashboardCardGroup data={groupsStats} loading={loading} />
          <DashboardCardCredential data={credentialsStats} loading={loading} />
        </div>
      </div>
      {tutorialIsRunning && <TutorialTool {...tutorialProgress} onSkip={onPauseTutorial} />}
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
    }).isRequired,
    configuration: PropTypes.shape({
      tutorialProgress: PropTypes.shape({
        started: PropTypes.bool.isRequired,
        paused: PropTypes.bool.isRequired,
        finished: PropTypes.bool.isRequired,
        basicSteps: PropTypes.number.isRequired,
        contacts: PropTypes.number.isRequired,
        groups: PropTypes.number.isRequired,
        credentials: PropTypes.number.isRequired
      }).isRequired,
      saveTutorialProgress: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  name: PropTypes.string,
  bundle: PropTypes.shape({
    remaining: PropTypes.number,
    totalConnections: PropTypes.number
  })
};

export default withApi(Dashboard);
