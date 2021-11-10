import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useLocation } from 'react-router-dom';
import { message } from 'antd';
import Logger from '../../helpers/Logger';
import Contact from './Contact';
import {
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';
import { useApi } from '../../hooks/useApi';

const ContactContainer = () => {
  const { t } = useTranslation();
  const {
    wallet,
    contactsManager,
    groupsManager,
    credentialsManager,
    credentialsReceivedManager
  } = useApi();

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
    contactsManager
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
  }, [loading.contact, contactsManager, id, t, setLoadingByKey]);

  const getGroups = useCallback(() => {
    if (loading.groups) return;
    setLoadingByKey('groups', true);
    groupsManager
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
  }, [loading.groups, groupsManager, id, t, setLoadingByKey]);

  const getIssuedCredentials = useCallback(() => {
    if (loading.issuedCredentials) return;
    setLoadingByKey('issuedCredentials', true);
    credentialsManager
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
  }, [loading.issuedCredentials, credentialsManager, id, t, setLoadingByKey]);

  useEffect(() => {
    if (!contact) getContact();
    if (!groups) getGroups();
    if (!issuedCredentials) getIssuedCredentials();
  }, [contact, groups, issuedCredentials, getContact, getGroups, getIssuedCredentials]);

  const getReceivedCredentials = useCallback(() => {
    if (loading.receivedCredentials) return;
    setLoadingByKey('receivedCredentials', true);
    credentialsReceivedManager
      .getReceivedCredentials(id)
      .then(credentials => {
        const credentialPromises = credentials.map(credential =>
          credentialsManager
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
    credentialsReceivedManager,
    credentialsManager,
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
      ? wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const onDeleteGroup = (groupId, contactIdsToRemove) =>
    groupsManager
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
    contactsManager
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

export default ContactContainer;
