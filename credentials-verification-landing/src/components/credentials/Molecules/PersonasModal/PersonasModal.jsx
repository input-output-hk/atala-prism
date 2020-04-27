import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Modal } from 'antd';
import PersonaCard from '../PersonaCard/PersonaCard';
import studentProfilePic from '../../../../images/atala-form-bg.png';
import touristProfilePic from '../../../../images/atala-form-bg.png';
import executiveProfilePic from '../../../../images/atala-form-bg.png';

import './_style.scss';

const PersonasModal = ({ showModal, selectPersona }) => {
  const { t } = useTranslation();

  const personas = [
    {
      name: t('credentials.personasModal.studentPersona.name'),
      profilePic: studentProfilePic,
      profilePicAlt: `${t('credentials.personasModal.studentPersona.name')} profile picture`,
      dateOfBirth: '1999-01-11T14:21:29.326Z',
      description: t('credentials.personasModal.studentPersona.description'),
      history: t('credentials.personasModal.studentPersona.history'),
      type: t('credentials.personasModal.studentPersona.type'),
      pronoun: t('credentials.personasModal.studentPersona.pronoun'),
      credentials: [
        t('credential.credentialNames.CredentialType0'),
        t('credential.credentialNames.CredentialType1'),
        t('credential.credentialNames.CredentialType2'),
        t('credential.credentialNames.CredentialType3')
      ],
      disabled: false
    },
    {
      name: t('credentials.personasModal.touristPersona.name'),
      profilePic: touristProfilePic,
      profilePicAlt: `${t('credentials.personasModal.touristPersona.name')} profile picture`,
      dateOfBirth: '1996-04-02T14:21:29.326Z',
      description: t('credentials.personasModal.touristPersona.description'),
      history: t('credentials.personasModal.touristPersona.history'),
      type: t('credentials.personasModal.touristPersona.type'),
      pronoun: t('credentials.personasModal.touristPersona.pronoun'),
      credentials: [
        t('credential.credentialNames.tourist.CredentialType0'),
        t('credential.credentialNames.tourist.CredentialType1'),
        t('credential.credentialNames.tourist.CredentialType2'),
        t('credential.credentialNames.tourist.CredentialType3')
      ],
      disabled: true
    },
    {
      name: t('credentials.personasModal.executivePersona.name'),
      name: t('credentials.personasModal.executivePersona.name'),
      profilePic: executiveProfilePic,
      profilePicAlt: `${t('credentials.personasModal.executivePersona.name')} profile picture`,
      dateOfBirth: '1984-06-03T14:21:29.326Z',
      description: t('credentials.personasModal.executivePersona.description'),
      history: t('credentials.personasModal.executivePersona.history'),
      type: t('credentials.personasModal.executivePersona.type'),
      pronoun: t('credentials.personasModal.executivePersona.pronoun'),
      credentials: [
        t('credential.credentialNames.executive.CredentialType0'),
        t('credential.credentialNames.executive.CredentialType1'),
        t('credential.credentialNames.executive.CredentialType2'),
        t('credential.credentialNames.executive.CredentialType3')
      ],
      disabled: true
    }
  ];

  const personaCards = personas.map(persona => (
    <PersonaCard {...persona} selectPersona={selectPersona} />
  ));

  return (
    <Modal visible={showModal} footer={null} closable={false}>
      <div className="PersonasModal">
        <h1>{t('credentials.personasModal.title')}</h1>
        <div className="PersonaCards">{personaCards}</div>
      </div>
    </Modal>
  );
};

PersonasModal.propTypes = {
  showModal: PropTypes.bool.isRequired,
  selectPersona: PropTypes.func.isRequired
};

export default PersonasModal;
