import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const PersonaCard = ({
  profilePic,
  profilePicAlt,
  name,
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
      <p>{name}</p>
      <p>{description}</p>
      <p>{history}</p>
      <p>{`${t('credentials.personasModal.joinAtala')} ${name} ${t(
        'credentials.personasModal.joinAtala1'
      )} ${pronoun} ${t('credentials.personasModal.joinAtala2')}`}</p>
      <div>
        <p>{type}</p>
        {personaCredentials}
      </div>
      <CustomButton
        buttonProps={{
          className: 'theme-primary',
          onClick: () => selectPersona({ name, dateOfBirth }),
          disabled: disabled
        }}
        buttonText={
          disabled
            ? t('credentials.personasModal.disabledButton')
            : `${name}${t('credentials.personasModal.enabledButton')}`
        }
      />
    </div>
  );
};

PersonaCard.propTypes = {
  profilePic: PropTypes.string.isRequired,
  profilePicAlt: PropTypes.string.isRequired,
  dateOfBirth: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  history: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
  credentials: PropTypes.arrayOf().isRequired,
  selectPersona: PropTypes.func.isRequired
};

export default PersonaCard;
