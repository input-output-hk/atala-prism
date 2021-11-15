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
  const { wallet } = useApi();

  const { id } = useParams();
  const {
    contactIsLoaded,
    removeFromGroup: handleRemoveFromGroup,
    updateContact: handleUpdateContact
  } = useCurrentContactState(id);

  const { search } = useLocation();
  const query = new URLSearchParams(search);
  const editing = query.get('editing');

  const handleVerifyCredential = ({ encodedSignedCredential, batchInclusionProof }) =>
    batchInclusionProof
      ? wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const isEditing = contactIsLoaded && editing;

  return (
    <Contact
      isEditing={isEditing}
      onVerifyCredential={handleVerifyCredential}
      onRemoveFromGroup={handleRemoveFromGroup}
      onUpdateContact={handleUpdateContact}
    />
  );
});

export default ContactContainer;
