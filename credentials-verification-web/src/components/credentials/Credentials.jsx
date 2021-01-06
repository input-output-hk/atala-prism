import React, { useState } from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialsFilter from './Molecules/Filters/CredentialsFilter/CredentialsFilter';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryDetail';
import { credentialTabShape } from '../../helpers/propShapes';
import CreateCredentialsButton from './Atoms/Buttons/CreateCredentialsButton';
import CredentialsIssued from './Organisms/Tabs/CredentialsIssued';
import CredentialsReceived from './Organisms/Tabs/CredentialsReceived';
import { CREDENTIALS_ISSUED, CREDENTIALS_RECEIVED } from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';

import './_style.scss';
import { useSession } from '../providers/SessionContext';

const { TabPane } = Tabs;

const Credentials = ({ tabProps, setActiveTab, loading }) => {
  const { t } = useTranslation();
  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);

  const { accountIsConfirmed } = useSession();

  const showCredentialData = credential => {
    setCurrentCredential(credential);
    setShowDrawer(true);
  };

  return (
    <div className="Wrapper PageContainer CredentialsTableContainer">
      {!accountIsConfirmed && <WaitBanner />}
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
        {accountIsConfirmed && <CreateCredentialsButton />}
      </div>
      <Tabs defaultActiveKey={CREDENTIALS_ISSUED} onChange={setActiveTab}>
        <TabPane key={CREDENTIALS_ISSUED} tab={t('credentials.tabs.credentialsIssued')}>
          <CredentialsFilter {...tabProps[CREDENTIALS_ISSUED]} isIssued />
          <CredentialsIssued
            {...tabProps[CREDENTIALS_ISSUED]}
            showCredentialData={showCredentialData}
            initialLoading={loading.issued}
          />
        </TabPane>
        <TabPane key={CREDENTIALS_RECEIVED} tab={t('credentials.tabs.credentialsReceived')}>
          {/* disabled for now */}
          {/* <CredentialsFilter {...tabProps[CREDENTIALS_RECEIVED]} /> */}
          <CredentialsReceived
            {...tabProps[CREDENTIALS_RECEIVED]}
            showCredentialData={showCredentialData}
            initialLoading={loading.received}
          />
        </TabPane>
      </Tabs>
    </div>
  );
};

Credentials.defaultProps = {
  loading: {
    issued: false,
    received: false
  }
};

Credentials.propTypes = {
  tabProps: PropTypes.shape({
    [CREDENTIALS_ISSUED]: PropTypes.shape(credentialTabShape),
    [CREDENTIALS_RECEIVED]: PropTypes.shape(credentialTabShape)
  }).isRequired,
  loading: PropTypes.shape({ issued: PropTypes.bool, received: PropTypes.bool }),
  setActiveTab: PropTypes.func.isRequired
};

export default Credentials;
