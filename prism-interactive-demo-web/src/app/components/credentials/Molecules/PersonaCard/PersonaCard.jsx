import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import CustomButton from '../../../../../components/customButton/CustomButton';

import './_style.scss';

const PersonaCard = ({
  title,
  profilePic,
  profilePicAlt,
  shortName,
  completeName,
  dateOfBirth,
  description,
  history,
  type,
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
        <h3>{title}</h3>
        <p>{history}</p>
        <div className="CredentialType">
          <h3>{type}</h3>
          {personaCredentials}
        </div>
        <p>
          <strong>{description}</strong>
        </p>
        <CustomButton
          buttonProps={{
            className: 'theme-primary',
            onClick: () => selectPersona({ firstName: completeName, dateOfBirth }),
            disabled
          }}
          buttonText={
            disabled
              ? t('credentials.personasModal.disabledButton')
              : `${shortName}${t('credentials.personasModal.enabledButton')}`
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
  shortName: PropTypes.string.isRequired,
  completeName: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  history: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
  credentials: PropTypes.arrayOf().isRequired,
  selectPersona: PropTypes.func.isRequired
};

export default PersonaCard;
