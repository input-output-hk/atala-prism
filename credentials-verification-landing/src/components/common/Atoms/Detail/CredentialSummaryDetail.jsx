import React, { Fragment } from 'react';
import { Drawer, Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialSummaryData from '../CredentialData/CredentialSummaryData';
import CustomButton from '../CustomButton/CustomButton';
import {
  drawerWidth,
  VERIFIER,
  USER_ROLE,
  ORGANISATION_NAME,
  RIGHT
} from '../../../../helpers/constants';

const CredentialSummaryDetail = ({ drawerInfo, credentialData }) => {
  const { t } = useTranslation();
  const role = localStorage.getItem(USER_ROLE);
  const university = localStorage.getItem(ORGANISATION_NAME);

  return (
    <Drawer placement="right" width={drawerWidth} destroyOnClose {...drawerInfo}>
      <Fragment>
        {credentialData && <CredentialSummaryData {...credentialData} university={university} />}
        {role === VERIFIER && (
          <CustomButton
            buttonProps={{ className: 'theme-outline', onClick: () => {} }}
            icon={{ icon: <Icon type="download" />, side: RIGHT }}
          />
        )}
        {role === VERIFIER && (
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: () => {} }}
            buttonText={t('credentials.detail.verify')}
          />
        )}
      </Fragment>
    </Drawer>
  );
};

CredentialSummaryDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credentialData: PropTypes.shape().isRequired
};

export default CredentialSummaryDetail;
