import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import refreshIcon from '../../../../images/Refresh.svg';

import './_style.scss';

const CredentialsButtons = ({
  refreshCredentials,
  signSelectedCredentials,
  sendSelectedCredentials,
  disableSign,
  disableSend
}) => {
  const { t } = useTranslation();

  const [loadingSignSelected, setLoadingSignSelected] = useState(false);
  const [loadingSendSelected, setLoadingSendSelected] = useState(false);

  const handleSignSelectedCredentials = async () => {
    setLoadingSignSelected(true);
    await signSelectedCredentials();
    setLoadingSignSelected(false);
  };

  const handleSendSelectedCredentials = async () => {
    setLoadingSendSelected(true);
    await sendSelectedCredentials();
    setLoadingSendSelected(false);
  };

  return (
    <div className="ControlButtons CredentialsOptions">
      <div className="BulkOptions">
        <Button className="RefreshButton" onClick={refreshCredentials}>
          <img src={refreshIcon} alt="refresh" />
          Refresh Table
        </Button>
        <CustomButton
          buttonProps={{
            className: 'buttonSignSelected theme-outline',
            onClick: handleSignSelectedCredentials,
            disabled: disableSign
          }}
          loading={loadingSignSelected}
          buttonText={t('credentials.actions.signSelectedCredentials')}
        />
        <CustomButton
          buttonProps={{
            className: 'buttonSignSelected theme-outline',
            onClick: handleSendSelectedCredentials,
            disabled: disableSend
          }}
          loading={loadingSendSelected}
          buttonText={t('credentials.actions.sendSelectedCredentials')}
        />
      </div>
    </div>
  );
};

CredentialsButtons.defaultProps = {
  disableSign: true,
  disableSend: true
};

CredentialsButtons.propTypes = {
  refreshCredentials: PropTypes.func.isRequired,
  signSelectedCredentials: PropTypes.func.isRequired,
  sendSelectedCredentials: PropTypes.func.isRequired,
  disableSign: PropTypes.bool,
  disableSend: PropTypes.bool
};

export default withRedirector(CredentialsButtons);
