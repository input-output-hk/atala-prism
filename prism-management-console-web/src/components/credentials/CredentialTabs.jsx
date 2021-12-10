import React, { useState } from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryDetail';
import CreateCredentialsButton from './Atoms/Buttons/CreateCredentialsButton';
import CredentialsIssued from './Organisms/Tabs/CredentialsIssued';
import CredentialsReceived from './Organisms/Tabs/CredentialsReceived';
import {
  CONFIRMED,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECEIVED,
  UNCONFIRMED
} from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';

import './_style.scss';

const { TabPane } = Tabs;

const CredentialTabs = observer(({ credentialActionsProps }) => {
  const { t } = useTranslation();
  const [currentCredential, setCurrentCredential] = useState();
  const [showDrawer, setShowDrawer] = useState(false);

  const { accountStatus } = useSession();

  const { verifyCredential } = credentialActionsProps;

  const showCredentialData = async credential => {
    const verificationResult = await verifyCredential(credential);
    setCurrentCredential({ verificationResult, ...credential });
    setShowDrawer(true);
  };

  return (
    <div className="Wrapper PageContainer CredentialsTableContainer">
      {accountStatus === UNCONFIRMED && <WaitBanner />}
      {currentCredential && (
        <CredentialSummaryDetail
          drawerInfo={{
            title: t('credentials.detail.title'),
            onClose: () => setShowDrawer(false),
            visible: showDrawer
          }}
          credential={currentCredential}
        />
      )}
      <div className="ContentHeader">
        <div>
          <h1>{t('credentials.title')}</h1>
          <h3>{t('credentials.info')}</h3>
        </div>
        {accountStatus === CONFIRMED && <CreateCredentialsButton />}
      </div>
      <div className="tabContent">
        <Tabs defaultActiveKey={CREDENTIALS_ISSUED}>
          <TabPane key={CREDENTIALS_ISSUED} tab={t('credentials.tabs.credentialsIssued')}>
            <CredentialsIssued
              credentialActionsProps={credentialActionsProps}
              showCredentialData={showCredentialData}
            />
          </TabPane>
          <TabPane key={CREDENTIALS_RECEIVED} tab={t('credentials.tabs.credentialsReceived')}>
            <CredentialsReceived showCredentialData={showCredentialData} />
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
});

CredentialTabs.propTypes = {
  credentialActionsProps: PropTypes.shape({
    verifyCredential: PropTypes.func.isRequired,
    revokeSingleCredential: PropTypes.func.isRequired,
    signSingleCredential: PropTypes.func.isRequired,
    sendSingleCredential: PropTypes.func.isRequired,
    bulkActionsProps: PropTypes.shape({
      refreshCredentials: PropTypes.func.isRequired,
      revokeSelectedCredentials: PropTypes.func.isRequired,
      signSelectedCredentials: PropTypes.func.isRequired,
      sendSelectedCredentials: PropTypes.func.isRequired
    })
  }).isRequired
};

export default CredentialTabs;
