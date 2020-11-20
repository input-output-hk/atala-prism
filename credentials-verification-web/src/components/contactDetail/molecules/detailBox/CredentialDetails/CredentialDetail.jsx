import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import CredentialSummaryDetail from '../../../../common/Organisms/Detail/CredentialSummaryDetail';
import contactIcon from '../../../../../images/holder-default-avatar.svg';
import { mockHtmlCredential } from '../../../../../helpers/mockData';
import './style.scss';

const CredentialDetail = ({ credential }) => {
  const { img, credentialName, title, date } = credential;
  const { t } = useTranslation();

  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);

  const showCredentialData = clickedCredential => {
    setCurrentCredential(clickedCredential);
    setShowDrawer(true);
  };

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
          <img className="icons" src={img} alt={credentialName} />
        </div>
        <div className="credentialData">
          <p>{credentialName}</p>
          <span>{title}</span>
        </div>
      </div>
      <div className="credentialDataContainer">
        <div className="credentialData">
          <p>{t('credentials.detail.dateCreated')}</p>
          <span>{date}</span>
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
  img: contactIcon,
  credentialName: 'Government ID',
  title: 'Government ID',
  date: '10/11/2020',
  html: mockHtmlCredential
};

CredentialDetail.defaultProps = {
  credential: mockCredential
};

CredentialDetail.propTypes = {
  credential: PropTypes.shape({
    img: PropTypes.string,
    credentialName: PropTypes.string,
    title: PropTypes.string,
    date: PropTypes.string,
    html: PropTypes.string
  })
};

export default CredentialDetail;
