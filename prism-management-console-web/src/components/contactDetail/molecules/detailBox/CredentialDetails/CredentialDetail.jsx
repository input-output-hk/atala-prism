import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import CredentialSummaryDetail from '../../../../common/Organisms/Detail/CredentialSummaryDetail';
import { backendDateFormat } from '../../../../../helpers/formatters';
import { credentialReceivedShape, credentialShape } from '../../../../../helpers/propShapes';

import './style.scss';

const CredentialDetail = ({ credential, isCredentialIssued, onVerifyCredential }) => {
  const { publicationStoredAt } = credential;
  const { t } = useTranslation();

  const [currentCredential, setCurrentCredential] = useState();
  const [showDrawer, setShowDrawer] = useState(false);

  const showCredentialData = async clickedCredential => {
    const verificationResult = await onVerifyCredential(clickedCredential);
    setCurrentCredential({ verificationResult, ...clickedCredential });
    setShowDrawer(true);
  };

  const getCredentialTypeAttributes = () => ({
    credentialTypeName: credential.credentialData.credentialTypeDetails.name,
    credentialTypeIcon: credential.credentialData.credentialTypeDetails.icon
  });

  const { credentialTypeName, credentialTypeIcon } = getCredentialTypeAttributes();

  const renderDateSigned = () => (
    <div className="credentialData">
      <p>{t('credentials.detail.dateSigned')}</p>
      <span>
        {publicationStoredAt?.seconds
          ? backendDateFormat(publicationStoredAt?.seconds)
          : t('credentials.detail.notPublished')}
      </span>
    </div>
  );

  return (
    <div className="credentialDetailContainer">
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
      <div className="credentialDataContainer">
        <div className="img">
          <img className="icons" src={credentialTypeIcon} alt={credentialTypeName || ''} />
        </div>
        <div className="credentialData">
          <p>{t('credentials.table.columns.credentialType')}</p>
          <span>{credentialTypeName}</span>
        </div>
      </div>
      <div className="credentialDataContainer">
        {isCredentialIssued && renderDateSigned()}
        <div className="ml">
          <CustomButton
            buttonProps={{
              className: 'theme-link',
              onClick: () => showCredentialData(credential)
            }}
            buttonText={t('actions.view')}
          />
        </div>
      </div>
    </div>
  );
};

CredentialDetail.defaultProps = {
  isCredentialIssued: false
};

CredentialDetail.propTypes = {
  credential: PropTypes.oneOfType([credentialShape, credentialReceivedShape]).isRequired,
  isCredentialIssued: PropTypes.bool,
  onVerifyCredential: PropTypes.func.isRequired
};

export default CredentialDetail;
