import React, { useState } from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialsFilter from './Molecules/Filters/CredentialsFilter/CredentialsFilter';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryDetail';
import { credentialTabShape } from '../../helpers/propShapes';
import CreateCredentialsButton from './Atoms/Buttons/CreateCredentialsButton';
import CredentialsIssued from './Organisms/Tabs/CredentialsIssued';
import CredentialsRecieved from './Organisms/Tabs/CredentialsRecieved';
import { CREDENTIALS_ISSUED, CREDENTIALS_RECIEVED } from '../../helpers/constants';

import './_style.scss';

const { TabPane } = Tabs;

const Credentials = ({ tabProps, setActiveTab, initialLoading }) => {
  const { t } = useTranslation();
  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);

  const showCredentialData = credential => {
    setCurrentCredential(credential);
    setShowDrawer(true);
  };

  return (
    <div className="Wrapper PageContainer CredentialsTableContainer">
      <CredentialSummaryDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setShowDrawer(false),
          visible: showDrawer
        }}
        credentialData={currentCredential}
      />
      <div className="ContentHeader">
        <div>
          <h1>{t('credentials.title')}</h1>
          <h3>{t('credentials.info')}</h3>
        </div>
        <CreateCredentialsButton />
      </div>
      <Tabs defaultActiveKey={CREDENTIALS_ISSUED} onChange={setActiveTab}>
        <TabPane key={CREDENTIALS_ISSUED} tab={t('credentials.tabs.credentialsIssued')}>
          <CredentialsFilter {...tabProps[CREDENTIALS_ISSUED]} />
          <CredentialsIssued
            {...tabProps[CREDENTIALS_ISSUED]}
            showCredentialData={showCredentialData}
            initialLoading={initialLoading}
          />
        </TabPane>
        <TabPane key={CREDENTIALS_RECIEVED} tab={t('credentials.tabs.credentialsRecieved')}>
          <CredentialsFilter {...tabProps[CREDENTIALS_RECIEVED]} />
          <CredentialsRecieved
            {...tabProps[CREDENTIALS_RECIEVED]}
            showCredentialData={showCredentialData}
            initialLoading={initialLoading}
          />
        </TabPane>
      </Tabs>
    </div>
  );
};

Credentials.defaultProps = {
  initialLoading: false
};

Credentials.propTypes = {
  tabProps: PropTypes.shape({
    [CREDENTIALS_ISSUED]: PropTypes.shape(credentialTabShape),
    [CREDENTIALS_RECIEVED]: PropTypes.shape(credentialTabShape)
  }).isRequired,
  initialLoading: PropTypes.bool,
  setActiveTab: PropTypes.func.isRequired
};

export default Credentials;
