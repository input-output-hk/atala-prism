import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Modal } from 'antd';
import PersonaCard from '../PersonaCard/PersonaCard';
import studentProfilePic from '../../../../images/atala-avatar-jo.png';
import touristProfilePic from '../../../../images/atala-avatar-grace.png';
import executiveProfilePic from '../../../../images/atala-avatar-brandon.png';

import './_style.scss';

const PersonasModal = ({ showModal, selectPersona }) => {
  const { t } = useTranslation();

  const personas = [
    {
      title: t('credentials.personasModal.studentPersona.title'),
      shortName: t('credentials.personasModal.studentPersona.shortName'),
      completeName: t('credentials.personasModal.studentPersona.completeName'),
      profilePic: studentProfilePic,
      profilePicAlt: `${t('credentials.personasModal.studentPersona.name')} profile picture`,
      dateOfBirth: '1999-01-11T14:21:29.326Z',
      description: t('credentials.personasModal.studentPersona.description'),
      history: t('credentials.personasModal.studentPersona.history'),
      type: t('credentials.personasModal.studentPersona.type'),
      credentials: [
        t('credential.credentialNames.CredentialType0'),
        t('credential.credentialNames.CredentialType1'),
        t('credential.credentialNames.CredentialType2'),
        t('credential.credentialNames.CredentialType3')
      ],
      disabled: false
    },
    {
      title: t('credentials.personasModal.touristPersona.title'),
      shortName: t('credentials.personasModal.touristPersona.shortName'),
      completeName: t('credentials.personasModal.touristPersona.completeName'),
      profilePic: touristProfilePic,
      profilePicAlt: `${t('credentials.personasModal.touristPersona.name')} profile picture`,
      dateOfBirth: '1996-04-02T14:21:29.326Z',
      description: t('credentials.personasModal.touristPersona.description'),
      history: t('credentials.personasModal.touristPersona.history'),
      type: t('credentials.personasModal.touristPersona.type'),
      credentials: [
        t('credential.credentialNames.tourist.CredentialType0'),
        t('credential.credentialNames.tourist.CredentialType1'),
        t('credential.credentialNames.tourist.CredentialType2'),
        t('credential.credentialNames.tourist.CredentialType3')
      ],
      disabled: true
    },
    {
      title: t('credentials.personasModal.executivePersona.title'),
      shortName: t('credentials.personasModal.executivePersona.shortName'),
      completeName: t('credentials.personasModal.executivePersona.completeName'),
      profilePic: executiveProfilePic,
      profilePicAlt: `${t('credentials.personasModal.executivePersona.name')} profile picture`,
      dateOfBirth: '1984-06-03T14:21:29.326Z',
      description: t('credentials.personasModal.executivePersona.description'),
      history: t('credentials.personasModal.executivePersona.history'),
      type: t('credentials.personasModal.executivePersona.type'),
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
    <Modal visible={showModal} footer={null} closable={false} width="75%" style={{ top: 0 }}>
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
