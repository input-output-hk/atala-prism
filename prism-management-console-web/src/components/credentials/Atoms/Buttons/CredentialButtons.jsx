import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { RedoOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';
import {
  REVOKE_CREDENTIALS,
  SEND_CREDENTIALS,
  SIGN_CREDENTIALS
} from '../../../../helpers/constants';

const CredentialButtons = ({
  refreshCredentials,
  revokeSelectedCredentials,
  signSelectedCredentials,
  sendSelectedCredentials,
  disableRevoke,
  disableSign,
  disableSend
}) => {
  const { t } = useTranslation();

  const [loadingByKey, setLoadingByKey] = useState(null);

  const handleRevokeSelectedCredentials = async () => {
    setLoadingByKey(REVOKE_CREDENTIALS);
    await revokeSelectedCredentials();
    setLoadingByKey(null);
  };

  const handleSignSelectedCredentials = async () => {
    setLoadingByKey(SIGN_CREDENTIALS);
    await signSelectedCredentials();
    setLoadingByKey(null);
  };

  const handleSendSelectedCredentials = async () => {
    setLoadingByKey(SEND_CREDENTIALS);
    await sendSelectedCredentials();
    setLoadingByKey(null);
  };

  return (
    <div className="ControlButtons CredentialsOptions">
      <div className="BulkOptions">
        <CustomButton
          buttonProps={{
            className: 'BulkActionButton theme-outline',
            onClick: handleRevokeSelectedCredentials,
            disabled: disableRevoke
          }}
          loading={loadingByKey === REVOKE_CREDENTIALS}
          buttonText={t('credentials.actions.revokeSelectedCredentials')}
        />
        <CustomButton
          buttonProps={{
            className: 'BulkActionButton theme-outline',
            onClick: handleSignSelectedCredentials,
            disabled: disableSign
          }}
          loading={loadingByKey === SIGN_CREDENTIALS}
          buttonText={t('credentials.actions.signSelectedCredentials')}
        />
        <CustomButton
          buttonProps={{
            className: 'BulkActionButton theme-outline',
            onClick: handleSendSelectedCredentials,
            disabled: disableSend
          }}
          loading={loadingByKey === SEND_CREDENTIALS}
          buttonText={t('credentials.actions.sendSelectedCredentials')}
        />
      </div>
      <Button className="RefreshButton" icon={<RedoOutlined />} onClick={refreshCredentials}>
        {t('credentials.actions.refreshTable')}
      </Button>
    </div>
  );
};

CredentialButtons.defaultProps = {
  disableRevoke: true,
  disableSign: true,
  disableSend: true
};

CredentialButtons.propTypes = {
  refreshCredentials: PropTypes.func.isRequired,
  revokeSelectedCredentials: PropTypes.func.isRequired,
  signSelectedCredentials: PropTypes.func.isRequired,
  sendSelectedCredentials: PropTypes.func.isRequired,
  disableRevoke: PropTypes.bool,
  disableSign: PropTypes.bool,
  disableSend: PropTypes.bool
};

export default withRedirector(CredentialButtons);
