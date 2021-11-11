import React from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useLocation } from 'react-router-dom';
import { message } from 'antd';
import { observer } from 'mobx-react-lite';
import Logger from '../../helpers/Logger';
import Contact from './Contact';
import {
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';
import { useApi } from '../../hooks/useApi';
import { useCurrentContactState } from '../../hooks/useCurrentContactState';

const ContactContainer = observer(() => {
  const { t } = useTranslation();
  const { wallet, groupsManager, contactsManager } = useApi();

  const { id } = useParams();
  const { isLoadingContact, isLoadingGroups, loadContact, loadGroups } = useCurrentContactState(id);

  const { search } = useLocation();
  const query = new URLSearchParams(search);
  const editing = query.get('editing');

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

  const deleteGroup = (groupId, contactIdsToRemove) =>
    groupsManager
      .updateGroup(groupId, { contactIdsToRemove })
      .then(() => {
        loadGroups();
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
        loadContact();
        message.success(t('contacts.edit.success.updating'));
      })
      .catch(error => {
        Logger.error(
          '[ContactContainer.updateContact] There has been an error updating the contact information',
          error
        );
        message.error(t('contacts.edit.error.updating'));
      });

  const isEditing = !isLoadingContact && !isLoadingGroups && editing;
  return (
    <Contact
      isEditing={isEditing}
      verifyCredential={verifyCredential}
      deleteGroup={deleteGroup}
      updateContact={updateContact}
    />
  );
});

export default ContactContainer;
