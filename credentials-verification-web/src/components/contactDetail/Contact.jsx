import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../providers/withRedirector';
import DetailBox from './molecules/detailBox/DetailBox';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import contactIcon from '../../images/holder-default-avatar.svg';
import CredentialDetail from './molecules/detailBox/CredentialDetails/CredentialDetail';

import './_style.scss';

const Contact = ({
  contact: { contactName, externalid },
  groups,
  redirector: { redirectToContacts }
}) => {
  const { t } = useTranslation();

  const credNumber = 3;

  return (
    <div className="contactDetail">
      <div className="ContentHeader headerSection">
        <h1>{t('contacts.detail.detailSection.title')}</h1>
      </div>
      <div className="detailSection">
        <div>
          <h3>{t('contacts.detail.detailSection.subtitle')}</h3>
          <div className="header">
            <div className="img">
              <img className="ContactIcon" src={contactIcon} alt="ContactIcon" />
            </div>
            <div className="title">
              <p>{t('contacts.table.columns.contactName')}</p>
              <span>{contactName}</span>
            </div>
            <div className="title">
              <p>{t('contacts.table.columns.externalid')}</p>
              <span>{externalid}</span>
            </div>
          </div>
          <p className="subtitle">{t('contacts.detail.detailSection.credentialSubtitle')}</p>
          <DetailBox groups={groups} />
        </div>
        <div className="CredentialInfo">
          <div className="CredentialTitleContainer">
            <span>
              {t('contacts.detail.credIssued')} ({credNumber})
            </span>
            <span>
              {t('contacts.detail.credReceived')} ({credNumber})
            </span>
          </div>
          <p>{t('contacts.detail.detailSection.groupsSubtitle')}</p>
          <div className="credentialDetailsContainer">
            <CredentialDetail />
          </div>
        </div>
      </div>
      <div className="buttonSection">
        <CustomButton
          buttonText="Back"
          buttonProps={{ className: 'theme-grey', onClick: redirectToContacts }}
        />
      </div>
    </div>
  );
};

Contact.defaultProps = {
  contact: {},
  groups: []
};

Contact.propTypes = {
  contact: PropTypes.shape({
    contactid: PropTypes.string,
    externalid: PropTypes.string,
    contactName: PropTypes.string,
    creationDate: PropTypes.shape({
      day: PropTypes.number,
      month: PropTypes.number,
      year: PropTypes.number
    }),
    connectionstatus: PropTypes.number,
    connectiontoken: PropTypes.string,
    connectionid: PropTypes.string,
    createdat: PropTypes.number
  }),
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberofcontacts: PropTypes.number
    })
  ),
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func }).isRequired
};

export default withRedirector(Contact);
