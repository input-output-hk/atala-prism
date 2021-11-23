import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialsFilter from '../../Molecules/Filters/CredentialsFilter/CredentialsFilter';
import {
  REVOKE_CREDENTIALS,
  SEND_CREDENTIALS,
  SIGN_CREDENTIALS
} from '../../../../helpers/constants';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import { useCredentialsIssuedPageStore } from '../../../../hooks/useCredentialsIssuedPageStore';

import './_style.scss';

const CredentialButtons = observer(
  ({
    revokeSelectedCredentials,
    signSelectedCredentials,
    sendSelectedCredentials,
    disableRevoke,
    disableSign,
    disableSend
  }) => {
    const { t } = useTranslation();
    const { hasAditionalFiltersApplied } = useCredentialsIssuedPageStore();

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
        <CredentialsFilter isIssued />
      </Menu>
    );

    return (
      <div className="ControlButtons CredentialsOptions">
        <CustomButton
          overrideClassName="BulkActionButton theme-outline"
          buttonProps={{
            onClick: handleRevokeSelectedCredentials,
            disabled: disableRevoke
          }}
          loading={loadingByKey === REVOKE_CREDENTIALS}
          buttonText={t('credentials.actions.revokeSelectedCredentials')}
        />
        <CustomButton
          overrideClassName="BulkActionButton theme-outline"
          buttonProps={{
            onClick: handleSignSelectedCredentials,
            disabled: disableSign
          }}
          loading={loadingByKey === SIGN_CREDENTIALS}
          buttonText={t('credentials.actions.signSelectedCredentials')}
        />
        <CustomButton
          overrideClassName="BulkActionButton theme-outline"
          buttonProps={{
            onClick: handleSendSelectedCredentials,
            disabled: disableSend
          }}
          loading={loadingByKey === SEND_CREDENTIALS}
          buttonText={t('credentials.actions.sendSelectedCredentials')}
        />

        <Badge dot={hasAditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
          <Dropdown.Button
            overlay={filtersMenu}
            trigger={['click']}
            className="SelectBtn theme-outline"
            icon={<FilterOutlined />}
          />
        </Badge>
      </div>
    );
  }
);

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
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
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

export default CredentialButtons;
