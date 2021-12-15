import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { Badge, Dropdown, Menu } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialsFilter from '../../Molecules/Filters/CredentialsFilter/CredentialsFilter';
import FilterIconComponent from '../../../common/Atoms/Icons/FilterIconComponent';
import {
  REVOKE_CREDENTIALS,
  SEND_CREDENTIALS,
  SIGN_CREDENTIALS
} from '../../../../helpers/constants';
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
    const {
      filterSortingProps: { hasAdditionalFiltersApplied }
    } = useCredentialsIssuedPageStore();

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

        <Badge dot={hasAdditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
          <Dropdown.Button
            overlay={filtersMenu}
            trigger={['click']}
            className="SelectBtn theme-outline"
            icon={<FilterIconComponent />}
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
  disableSend: PropTypes.bool
};

export default CredentialButtons;
