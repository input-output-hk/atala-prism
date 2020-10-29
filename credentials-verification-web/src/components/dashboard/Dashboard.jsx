import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import { DatePicker, Space } from 'antd';
import Welcome from './Atoms/Welcome/Welcome';
import CurrentBundle from './Atoms/CurrentBundle/CurrentBundle';
import ConnectionSummary from './Molecules/ConnectionSummary/ConnectionSummary';
import TransactionSummary from './Molecules/TransactionSummary/TransactionSummary';
import { getCurrentLanguage } from '../../helpers/languageUtils';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { useSession } from '../providers/SessionContext';
import DashboardCard from './organism/DashboardCard';
import DashboardCardGroup from './organism/DashboardCardGroup';
import DashboardCardCredential from './organism/DashboardCardCredential';
import './_style.scss';

const Dashboard = ({ name, bundle, credentials, proofRequests }) => {
  const { t } = useTranslation();

  const dateFormat = 'YYYY/MM/DD';

  const customFormat = value => `custom format: ${value.format(dateFormat)}`;

  moment.locale(getCurrentLanguage());

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
        <h1>{t('dashboard.titleBottom')}</h1>
        <h3>{t('dashboard.subtitle')}</h3>
        <div className="dateInputsContainer">
          <div className="dateSpanContainer">
            <span>{t('commonWords.from')}</span>
          </div>
          <div className="datePickerContainer">
            <DatePicker defaultValue={moment('2015/01/01', dateFormat)} format={dateFormat} />
          </div>
          <div className="dateSpanContainer">
            <span>{t('commonWords.to')}</span>
          </div>
          <div className="datePickerContainer">
            <DatePicker defaultValue={moment('2015/01/01', dateFormat)} format={dateFormat} />
          </div>
          <div className="applyButtonContainer">
            <CustomButton
              buttonProps={{
                className: 'theme-outline'
              }}
              buttonText="Apply"
            />
          </div>
        </div>
        <div className="dashboardCardContainer">
          <DashboardCard />
          <DashboardCardGroup />
          <DashboardCardCredential />
        </div>
      </div>
    </div>
  );
};

Dashboard.defaultProps = {
  bundle: undefined,
  name: 'Username',
  credentials: { week: 10, month: 60 },
  proofRequests: { week: 10, month: 60 }
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
  }),
  proofRequests: PropTypes.shape({
    week: PropTypes.number,
    month: PropTypes.number
  })
};

export default Dashboard;
