import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import Contact from './Contact';

const ContactContainer = ({ api }) => {
  const { t } = useTranslation();
  const { id } = useParams();

  const [contact, setContact] = useState();
  const [groups, setGroups] = useState();
  const [issuedCredentials, setIssuedCredentials] = useState();
  const [receivedCredentials, setReceivedCredentials] = useState();

  const [loading, setLoading] = useState({});
  const setLoadingByKey = (key, value) =>
    setLoading(previousLoading => ({ ...previousLoading, [key]: value }));

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  const getContact = () =>
    api.contactsManager
      .getContact(id)
      .then(({ jsondata, ...rest }) => {
        const contactData = Object.assign(JSON.parse(jsondata), rest);
        setContact(contactData);
      })
      .catch(error => {
        Logger.error(`[ContactContainer.getContact] Error while getting contact ${id}`, error);
        message.error(t('errors.errorGetting', { model: 'contact' }));
      })
      .finally(() => setLoadingByKey('contact', false));

  const getGroups = () =>
    api.groupsManager
      .getGroups(id)
      .then(setGroups)
      .catch(error => {
        Logger.error(
          `[ContactContainer.getGroups] Error while getting groups for contact ${id}`,
          error
        );
        message.error(t('errors.errorGetting', { model: 'groups' }));
      })
      .finally(() => setLoadingByKey('groups', false));

  const getIssuedCredentials = () =>
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

  const getReceivedCredentials = () =>
    api.credentialsReceivedManager
      .getReceivedCredentials(id)
      .then(credentials => {
        const credentialPromises = credentials.map(credential =>
          api.credentialsManager
            .getBlockchainData(credential.encodedsignedcredential)
            .then(issuanceproof => Object.assign({ issuanceproof }, credential))
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

  useEffect(() => {
    setLoading({ contact: true, groups: true, issuedCredentials: true, receivedCredentials: true });
    getContact();
    getGroups();
    getIssuedCredentials();
  }, []);

  useEffect(() => {
    if (contact?.connectionid) getReceivedCredentials();
    else setLoadingByKey('receivedCredentials', false);
  }, [contact]);

  return (
    <Contact
      loading={loading}
      contact={contact}
      groups={groups}
      issuedCredentials={issuedCredentials}
      receivedCredentials={receivedCredentials}
      credentialTypes={credentialTypes}
    />
  );
};

ContactContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func }),
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getContactCredentials: PropTypes.func,
      getCredentialTypes: PropTypes.func,
      getBlockchainData: PropTypes.func
    }),
    credentialsReceivedManager: PropTypes.shape({ getReceivedCredentials: PropTypes.func })
  }).isRequired
};

export default withApi(ContactContainer);
