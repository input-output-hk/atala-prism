import React, { Fragment } from 'react';
import { Drawer, Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialData from '../../Atoms/CredentialData/CredentialData';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { drawerWidth, ISSUER, VERIFIER } from '../../../../helpers/constants';

const CredentialDetail = ({ drawerInfo, credentialData }) => {
  const { t } = useTranslation();
  const role = localStorage.getItem('userRole');

  return (
    <Drawer placement="right" width={drawerWidth} destroyOnClose {...drawerInfo}>
      <Fragment>
        {credentialData && <CredentialData {...credentialData} />}
        {role === ISSUER && (
          <CustomButton
            buttonProps={{ className: 'theme-outline', onClick: () => {} }}
            buttonText={t('credentials.detail.delete')}
          />
        )}
        {role === ISSUER && (
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: () => {} }}
            buttonText={t('credentials.detail.resend')}
          />
        )}
        {role === VERIFIER && (
          <CustomButton
            buttonProps={{ className: 'theme-outline', onClick: () => {} }}
            icon={<Icon type="download" />}
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

CredentialDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credentialData: PropTypes.shape().isRequired
};

export default CredentialDetail;
