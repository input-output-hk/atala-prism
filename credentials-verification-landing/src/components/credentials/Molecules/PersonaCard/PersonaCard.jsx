import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const PersonaCard = ({
  profilePic,
  profilePicAlt,
  firstName,
  dateOfBirth,
  description,
  history,
  type,
  pronoun,
  credentials,
  disabled,
  selectPersona
}) => {
  const { t } = useTranslation();

  const personaCredentials = credentials.map(credential => (
    <div>
      <p>{credential}</p>
    </div>
  ));

  return (
    <div className={disabled ? 'PersonaCardDisabled' : 'PersonaCardEnabled'}>
      <img src={profilePic} alt={profilePicAlt} />
      <div className="CardContent">
        <h3>{firstName}</h3>
        <p>
          <strong>{description}</strong>
        </p>
        <p>{history}</p>
        <p>{`${t('credentials.personasModal.joinAtala')} ${firstName} ${t(
          'credentials.personasModal.joinAtala1'
        )} ${pronoun} ${t('credentials.personasModal.joinAtala2')}`}</p>
        <div className="CredentialType">
          <h3>{type}</h3>
          {personaCredentials}
        </div>
        <CustomButton
          buttonProps={{
            className: 'theme-primary',
            onClick: () => selectPersona({ firstName, dateOfBirth }),
            disabled: disabled
          }}
          buttonText={
            disabled
              ? t('credentials.personasModal.disabledButton')
              : `${firstName}${t('credentials.personasModal.enabledButton')}`
          }
        />
      </div>
      <div className="CardOverlay" />
    </div>
  );
};

PersonaCard.propTypes = {
  profilePic: PropTypes.string.isRequired,
  profilePicAlt: PropTypes.string.isRequired,
  dateOfBirth: PropTypes.string.isRequired,
  firstName: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  history: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
  credentials: PropTypes.arrayOf().isRequired,
  selectPersona: PropTypes.func.isRequired
};

export default PersonaCard;
