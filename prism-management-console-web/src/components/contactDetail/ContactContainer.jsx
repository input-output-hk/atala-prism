import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useLocation } from 'react-router-dom';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import Contact from './Contact';
import {
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';

const ContactContainer = ({ api }) => {
  const { t } = useTranslation();
  const { id } = useParams();
  const { search } = useLocation();
  const query = new URLSearchParams(search);
  const editing = query.get('editing');

  const [contact, setContact] = useState();
  const [groups, setGroups] = useState();
  const [issuedCredentials, setIssuedCredentials] = useState();
  const [receivedCredentials, setReceivedCredentials] = useState();

  const [loading, setLoading] = useState({});
  const setLoadingByKey = useCallback(
    (key, value) => setLoading(previousLoading => ({ ...previousLoading, [key]: value })),
    []
  );

  const getContact = useCallback(() => {
    if (loading.contact) return;
    setLoadingByKey('contact', true);
    api.contactsManager
      .getContact(id)
      .then(({ jsonData, ...rest }) => {
        const contactData = Object.assign(JSON.parse(jsonData), rest);
        setContact(contactData);
      })
      .catch(error => {
        Logger.error(`[ContactContainer.getContact] Error while getting contact ${id}`, error);
        message.error(t('errors.errorGetting', { model: 'contact' }));
      })
      .finally(() => setLoadingByKey('contact', false));
  }, [loading.contact, api.contactsManager, id, t, setLoadingByKey]);

  const getGroups = useCallback(() => {
    if (loading.groups) return;
    setLoadingByKey('groups', true);
    api.groupsManager
      .getGroups({ contactId: id })
      .then(({ groupsList }) => setGroups(groupsList))
      .catch(error => {
        Logger.error(
          `[ContactContainer.getGroups] Error while getting groups for contact ${id}`,
          error
        );
        message.error(t('errors.errorGetting', { model: 'groups' }));
      })
      .finally(() => setLoadingByKey('groups', false));
  }, [loading.groups, api.groupsManager, id, t, setLoadingByKey]);

  const getIssuedCredentials = useCallback(() => {
    if (loading.issuedCredentials) return;
    setLoadingByKey('issuedCredentials', true);
    api.credentialsManager
      .getContactCredentials(id)
      .then(setIssuedCredentials)
      .catch(error => {
        Logger.error(
          `[ContactContainer.getIssuedCredentials] Error while getting issued credentials for contact ${id}`,
          error
        );
        message.error(t('errors.errorGetting', { model: 'issued credentials' }));
      })
      .finally(() => setLoadingByKey('issuedCredentials', false));
  }, [loading.issuedCredentials, api.credentialsManager, id, t, setLoadingByKey]);

  useEffect(() => {
    if (!contact) getContact();
    if (!groups) getGroups();
    if (!issuedCredentials) getIssuedCredentials();
  }, [contact, groups, issuedCredentials, getContact, getGroups, getIssuedCredentials]);

  const getReceivedCredentials = useCallback(() => {
    if (loading.receivedCredentials) return;
    setLoadingByKey('receivedCredentials', true);
    api.credentialsReceivedManager
      .getReceivedCredentials(id)
      .then(credentials => {
        const credentialPromises = credentials.map(credential =>
          api.credentialsManager
            .getBlockchainData(credential.encodedSignedCredential)
            .then(issuanceProof => ({ issuanceProof, ...credential }))
        );
        return Promise.all(credentialPromises);
      })
      .then(setReceivedCredentials)
      .catch(error => {
        Logger.error(
          `[ContactContainer.getReceivedCredentials] Error while getting received credentials for contact ${id}`,
          error
        );
        message.error(t('errors.errorGetting', { model: 'received credentials' }));
      })
      .finally(() => setLoadingByKey('receivedCredentials', false));
  }, [
    loading.receivedCredentials,
    api.credentialsReceivedManager,
    api.credentialsManager,
    id,
    t,
    setLoadingByKey
  ]);

  useEffect(() => {
    if (!contact) return;
    if (contact.connectionId) getReceivedCredentials();
    else setLoadingByKey('receivedCredentials', false);
  }, [contact, getReceivedCredentials, setLoadingByKey]);

  const verifyCredential = ({ encodedSignedCredential, batchInclusionProof }) =>
    batchInclusionProof
      ? api.wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const onDeleteGroup = (groupId, contactIdsToRemove) =>
    api.groupsManager
      .updateGroup(groupId, { contactIdsToRemove })
      .then(() => {
        getGroups();
        message.success(t('contacts.edit.success.removingFromGroup'));
      })
      .catch(error => {
        Logger.error(
          '[ContactContainer.onDeleteGroup] There has been an error removing contact from group',
          error
        );
        message.error(t('contacts.edit.error.removingFromGroup'));
      });

  const updateContact = (contactId, newContactData) =>
    api.contactsManager
      .updateContact(contactId, newContactData)
      .then(() => {
        getContact();
        message.success(t('contacts.edit.success.updating'));
      })
      .catch(error => {
        Logger.error(
          '[ContactContainer.updateContact] There has been an error updating the contact information',
          error
        );
        message.error(t('contacts.edit.error.updating'));
      });

  const isEditing = contact && groups && editing;

  return (
    <Contact
      loading={loading}
      contact={contact}
      groups={groups}
      editing={isEditing}
      issuedCredentials={issuedCredentials}
      receivedCredentials={receivedCredentials}
      verifyCredential={verifyCredential}
      onDeleteGroup={onDeleteGroup}
      updateContact={updateContact}
    />
  );
};

ContactContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func, updateContact: PropTypes.func }),
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func, updateGroup: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getContactCredentials: PropTypes.func,
      getBlockchainData: PropTypes.func
    }),
    credentialsReceivedManager: PropTypes.shape({ getReceivedCredentials: PropTypes.func }),
    wallet: PropTypes.shape({ verifyCredential: PropTypes.func })
  }).isRequired
};

export default withApi(ContactContainer);
