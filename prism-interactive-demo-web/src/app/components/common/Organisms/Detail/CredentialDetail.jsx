import React, { Fragment } from 'react';
import { Drawer, Icon } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import CredentialData from '../../Atoms/CredentialData/CredentialData';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { drawerWidth, ISSUER, VERIFIER, USER_ROLE } from '../../../../helpers/constants';
import './_style.scss';

const CredentialDetail = ({ drawerInfo, credentialData }) => {
  const { t } = useTranslation();
  const role = localStorage.getItem(USER_ROLE);

  return (
    <Drawer
      placement="right"
      width={drawerWidth}
      destroyOnClose
      {...drawerInfo}
      className="CredentialDetailDrawer"
    >
      <div className="CredentialDetailContainer">
        {credentialData && <CredentialData {...credentialData} />}
        <div className="CredentialDetailButtons">
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
        </div>
        {role === VERIFIER && (
          <CustomButton
            buttonProps={{ className: 'theme-outline', onClick: () => {} }}
            buttonText={t('credentials.detail.downloadButton')}
          />
        )}
        {role === VERIFIER && (
          <div id="notarization" className="BlockchainLog">
            <h3>{t('credentials.detail.notarizationTitle')}</h3>
            <span>{t('credentials.detail.hashTitle')}</span>
            <a href="#notarization">
              <p className="TxHash">#a4b9c56412df3cd6dd89c0e6f2d18ab34075566186e2e9a</p>
            </a>
            <span>{t('credentials.detail.notarizationDate')}</span>
            <p>20-11-2019 11:20AM</p>
          </div>
        )}
      </div>
    </Drawer>
  );
};

CredentialDetail.propTypes = {
  drawerInfo: PropTypes.shape().isRequired,
  credentialData: PropTypes.shape().isRequired
};

export default CredentialDetail;
