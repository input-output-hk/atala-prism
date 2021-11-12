import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Tabs } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import { observer } from 'mobx-react-lite';
import DetailBox from './molecules/detailBox/DetailBox';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import contactIcon from '../../images/holder-default-avatar.svg';
import CredentialDetail from './molecules/detailBox/CredentialDetails/CredentialDetail';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { useTranslationWithPrefix } from '../../hooks/useTranslationWithPrefix';
import EditContactModal from './organisms/EditContactModal/EditContactModal';
import { useRedirector } from '../../hooks/useRedirector';
import { useCurrentContactState } from '../../hooks/useCurrentContactState';

import './_style.scss';

const { TabPane } = Tabs;

const ISSUED = 'issued';
const RECEIVED = 'received';

const Contact = observer(({ isEditing, verifyCredential, removeFromGroup, updateContact }) => {
  const { t } = useTranslation();
  const { redirectToContacts } = useRedirector();
  const {
    contactId,
    contactName,
    externalId,
    groups,
    credentialsIssued,
    credentialsReceived,
    isLoadingContact,
    isLoadingGroups,
    isLoadingCredentialsIssued,
    isLoadingCredentialsReceived
  } = useCurrentContactState();

  const tp = useTranslationWithPrefix('contacts.detail');
  const [modalIsOpen, setModalIsOpen] = useState(false);
  const [groupsToRemove, setGroupsToRemove] = useState([]);

  useEffect(() => {
    setModalIsOpen(isEditing);
  }, [isEditing]);

  const handleUpdateContact = async newContactData => {
    handleUpdateContactData(newContactData);
    await handleRemoveFromGroups();
    setModalIsOpen(false);
  };

  const handleUpdateContactData = newContactData => {
    const shouldUpdateContactData =
      newContactData.name !== contactName || newContactData.externalId !== externalId;

    if (shouldUpdateContactData) updateContact(contactId, newContactData);
  };

  const handleRemoveFromGroups = async () => {
    if (!groupsToRemove.length) return;
    const updateGroupsPromises = groupsToRemove.map(groupId => removeFromGroup(groupId, contactId));
    return Promise.all(updateGroupsPromises);
  };

  const handleSelectGroupsToRemove = group => {
    setGroupsToRemove(groupsToRemove.concat(group));
  };
  const handleCancel = () => {
    setModalIsOpen(false);
    setGroupsToRemove([]);
  };

  return (
    <div className="contactDetail">
      <EditContactModal
        visible={modalIsOpen}
        externalId={externalId}
        name={contactName}
        groups={groups.filter(g => !groupsToRemove.includes(g.id))}
        contactId={contactId}
        selectGroupsToRemove={handleSelectGroupsToRemove}
        onClose={handleCancel}
        onFinish={handleUpdateContact}
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
              <span>{isLoadingContact ? <SimpleLoading size="xs" /> : contactName}</span>
            </div>
            <div className="title">
              <p>{t('contacts.table.columns.externalId')}</p>
              <span>{isLoadingContact ? <SimpleLoading size="xs" /> : externalId}</span>
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
          <DetailBox groups={groups} loading={isLoadingGroups} />
        </div>
        <Tabs defaultActiveKey={ISSUED} className="CredentialInfo">
          <TabPane key={ISSUED} tab={`${tp('credIssued')} (${credentialsIssued.length})`}>
            <p>{tp('detailSection.credentialsIssuedSubtitle')}</p>
            <div className="CredentialsContainer">
              {isLoadingCredentialsIssued ? (
                <SimpleLoading size="xs" />
              ) : (
                credentialsIssued.map(credential => (
                  <CredentialDetail
                    credential={credential}
                    isCredentialIssued
                    verifyCredential={verifyCredential}
                  />
                ))
              )}
            </div>
          </TabPane>
          <TabPane key={RECEIVED} tab={`${tp('credReceived')} (${credentialsReceived.length})`}>
            <p>{tp('detailSection.credentialsReceivedSubtitle')}</p>
            <div className="CredentialsContainer">
              {isLoadingCredentialsReceived ? (
                <SimpleLoading size="xs" />
              ) : (
                credentialsReceived.map(credential => (
                  <CredentialDetail credential={credential} verifyCredential={verifyCredential} />
                ))
              )}
            </div>
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
});

Contact.defaultProps = {
  isEditing: false
};

Contact.propTypes = {
  isEditing: PropTypes.bool,
  verifyCredential: PropTypes.func.isRequired,
  removeFromGroup: PropTypes.func.isRequired,
  updateContact: PropTypes.func.isRequired
};

export default Contact;
