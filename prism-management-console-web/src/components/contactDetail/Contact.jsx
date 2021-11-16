import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Tabs } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import DetailBox from './molecules/detailBox/DetailBox';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import contactIcon from '../../images/holder-default-avatar.svg';
import CredentialDetail from './molecules/detailBox/CredentialDetails/CredentialDetail';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { useTranslationWithPrefix } from '../../hooks/useTranslationWithPrefix';
import { credentialShape } from '../../helpers/propShapes';
import EditContactModal from './organisms/EditContactModal/EditContactModal';

import './_style.scss';
import { useRedirector } from '../../hooks/useRedirector';

const { TabPane } = Tabs;

const ISSUED = 'issued';
const RECEIVED = 'received';

const Contact = ({
  contact: { name, externalId, contactId },
  groups,
  loading,
  editing,
  issuedCredentials,
  receivedCredentials,
  verifyCredential,
  onDeleteGroup,
  updateContact
}) => {
  const { t } = useTranslation();
  const { redirectToContacts } = useRedirector();

  const tp = useTranslationWithPrefix('contacts.detail');
  const [modalIsOpen, setModalIsOpen] = useState(false);

  useEffect(() => {
    setModalIsOpen(editing);
  }, [editing]);

  return (
    <div className="contactDetail">
      <EditContactModal
        visible={modalIsOpen}
        externalId={externalId}
        name={name}
        groups={groups}
        contactId={contactId}
        onDeleteGroup={onDeleteGroup}
        onClose={() => setModalIsOpen(false)}
        updateContact={updateContact}
      />
      <div className="ContentHeader headerSection">
        <div className="buttonSection">
          <CustomButton
            buttonText={t('actions.back')}
            buttonProps={{
              icon: <LeftOutlined />,
              className: 'theme-grey',
              onClick: redirectToContacts
            }}
          />
        </div>
        <h1>{tp('detailSection.title')}</h1>
      </div>
      <div className="detailSection">
        <div className="ContactInfo">
          <h3>{tp('detailSection.subtitle')}</h3>
          <div className="header">
            <div className="img">
              <img className="ContactIcon" src={contactIcon} alt="ContactIcon" />
            </div>
            <div className="title">
              <p>{t('contacts.table.columns.contactName')}</p>
              <span>{loading.contact ? <SimpleLoading size="xs" /> : name}</span>
            </div>
            <div className="title">
              <p>{t('contacts.table.columns.externalId')}</p>
              <span>{loading.contact ? <SimpleLoading size="xs" /> : externalId}</span>
            </div>
            <CustomButton
              buttonText={t('groups.table.buttons.edit')}
              buttonProps={{
                className: 'theme-link buttonEdit',
                onClick: () => setModalIsOpen(true)
              }}
            />
          </div>
          <p className="subtitleCredentials">{tp('detailSection.groupsSubtitle')}</p>
          <DetailBox groups={groups} loading={loading.groups} />
        </div>
        <Tabs defaultActiveKey={ISSUED} className="CredentialInfo">
          <TabPane key={ISSUED} tab={`${tp('credIssued')} (${issuedCredentials.length})`}>
            <p>{tp('detailSection.credentialsIssuedSubtitle')}</p>
            <div className="CredentialsContainer">
              {loading.issuedCredentials ? (
                <SimpleLoading size="xs" />
              ) : (
                issuedCredentials.map(credential => (
                  <CredentialDetail
                    credential={credential}
                    isCredentialIssued
                    verifyCredential={verifyCredential}
                  />
                ))
              )}
            </div>
          </TabPane>
          <TabPane key={RECEIVED} tab={`${tp('credReceived')} (${receivedCredentials.length})`}>
            <p>{tp('detailSection.credentialsReceivedSubtitle')}</p>
            <div className="CredentialsContainer">
              {loading.receivedCredentials ? (
                <SimpleLoading size="xs" />
              ) : (
                receivedCredentials.map(credential => (
                  <CredentialDetail credential={credential} verifyCredential={verifyCredential} />
                ))
              )}
            </div>
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
};

Contact.defaultProps = {
  contact: {},
  groups: [],
  issuedCredentials: [],
  receivedCredentials: [],
  loading: {
    contact: false,
    groups: false,
    issuedCredentials: false,
    receivedCredentials: false
  },
  editing: false
};

Contact.propTypes = {
  contact: PropTypes.shape({
    contactId: PropTypes.string,
    externalId: PropTypes.string,
    name: PropTypes.string,
    creationDate: PropTypes.shape({
      day: PropTypes.number,
      month: PropTypes.number,
      year: PropTypes.number
    }),
    connectionStatus: PropTypes.number,
    connectionToken: PropTypes.string,
    connectionId: PropTypes.string,
    createdAt: PropTypes.number
  }),
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberOfContacts: PropTypes.number
    })
  ),
  loading: PropTypes.shape({
    contact: PropTypes.bool,
    groups: PropTypes.bool,
    issuedCredentials: PropTypes.bool,
    receivedCredentials: PropTypes.bool
  }),
  editing: PropTypes.bool,
  issuedCredentials: PropTypes.arrayOf(credentialShape),
  receivedCredentials: PropTypes.arrayOf(credentialShape),
  verifyCredential: PropTypes.func.isRequired,
  onDeleteGroup: PropTypes.func.isRequired,
  updateContact: PropTypes.func.isRequired
};

export default Contact;
