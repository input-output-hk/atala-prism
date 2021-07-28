import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialsFilter from '../../Molecules/Filters/CredentialsFilter/CredentialsFilter';
import {
  REVOKE_CREDENTIALS,
  SEND_CREDENTIALS,
  SIGN_CREDENTIALS
} from '../../../../helpers/constants';

import './_style.scss';
import { credentialTypeShape } from '../../../../helpers/propShapes';

const CredentialButtons = ({
  revokeSelectedCredentials,
  signSelectedCredentials,
  sendSelectedCredentials,
  disableRevoke,
  disableSign,
  disableSend,
  filterProps
}) => {
  const { t } = useTranslation();
  const { name, date, credentialType, credentialStatus, contactStatus } = filterProps;
  const hasFiltersApplied = name || date || credentialType || credentialStatus || contactStatus;

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

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <CredentialsFilter filterProps={filterProps} isIssued />
    </Menu>
  );

  return (
    <div className="ControlButtons CredentialsOptions">
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

      <Badge dot={hasFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
        <Dropdown.Button
          overlay={filtersMenu}
          trigger={['click']}
          className="SelectBtn theme-outline"
          icon={<FilterOutlined />}
        />
      </Badge>
    </div>
  );
};

CredentialButtons.defaultProps = {
  disableRevoke: true,
  disableSign: true,
  disableSend: true
};

CredentialButtons.propTypes = {
  revokeSelectedCredentials: PropTypes.func.isRequired,
  signSelectedCredentials: PropTypes.func.isRequired,
  sendSelectedCredentials: PropTypes.func.isRequired,
  disableRevoke: PropTypes.bool,
  disableSign: PropTypes.bool,
  disableSend: PropTypes.bool,
  filterProps: PropTypes.shape({
    name: PropTypes.string,
    setName: PropTypes.func,
    credentialTypes: PropTypes.arrayOf(PropTypes.shape(credentialTypeShape)),
    credentialType: PropTypes.string,
    setCredentialType: PropTypes.func,
    credentialStatus: PropTypes.number,
    setCredentialStatus: PropTypes.func,
    contactStatus: PropTypes.string,
    setContactStatus: PropTypes.func,
    date: PropTypes.string,
    setDate: PropTypes.func
  }).isRequired
};

export default withRedirector(CredentialButtons);
