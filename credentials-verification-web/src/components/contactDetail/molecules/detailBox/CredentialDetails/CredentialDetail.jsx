import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import CredentialSummaryDetail from '../../../../common/Organisms/Detail/CredentialSummaryDetail';
import { mockHtmlCredential } from '../../../../../helpers/mockData';
import { dateFormat } from '../../../../../helpers/formatters';
import { credentialTypesShape } from '../../../../../helpers/propShapes';

import './style.scss';

const CredentialDetail = ({ credential, credentialTypes }) => {
  const { credentialType, publicationstoredat } = credential;
  const { t } = useTranslation();

  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);

  const showCredentialData = clickedCredential => {
    setCurrentCredential(clickedCredential);
    setShowDrawer(true);
  };

  const { name, logo: credentialLogo } = credentialTypes[credentialType];

  return (
    <div className="credentialDetailContainer">
      <CredentialSummaryDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setShowDrawer(false),
          visible: showDrawer
        }}
        credentialData={currentCredential}
      />
      <div className="credentialDataContainer">
        <div className="img">
          <img className="icons" src={credentialLogo} alt={credentialType?.name || ''} />
        </div>
        <div className="credentialData">
          <p>{t('credentials.table.columns.credentialType')}</p>
          <span>{t(name)}</span>
        </div>
      </div>
      <div className="credentialDataContainer">
        <div className="credentialData">
          <p>{t('credentials.detail.dateSigned')}</p>
          <span>
            {publicationstoredat
              ? dateFormat(publicationstoredat)
              : t('credentials.detail.notPublished')}
          </span>
        </div>
        <div>
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

const mockCredential = {
  credentialType: 'governmentId',
  publicationstoredat: 0,
  html: mockHtmlCredential
};

CredentialDetail.defaultProps = { credential: mockCredential };

CredentialDetail.propTypes = {
  credential: PropTypes.shape({
    credentialType: PropTypes.string,
    publicationstoredat: PropTypes.number,
    html: PropTypes.string
  }),
  credentialTypes: PropTypes.shape(credentialTypesShape).isRequired
};

export default CredentialDetail;
