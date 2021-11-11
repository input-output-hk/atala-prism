import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../../../components/customButton/CustomButton';
import ContactButton from '../../Atoms/ContactButton/ContactButton';
import { CREDENTIAL_TYPES } from '../../../../../helpers/constants';
import checkIcon from '../../../../images/icon-verified.svg';

import './_style.scss';

const indexToImgTrad = {
  0: { src: '/images/icon-credential-id.svg', alt: 'Id Credential' },
  1: { src: '/images/icon-credential-university.svg', alt: 'University Credential' },
  2: { src: '/images/icon-credential-employment.svg', alt: 'Employment Credential' },
  3: { src: '/images/icon-credential-insurance.svg', alt: 'Insurance Credential' }
};

const CredentialsList = ({
  changeCurrentCredential,
  availableCredential,
  showContactButton,
  toContactForm,
  showCongrats
}) => {
  const { t } = useTranslation();

  const credentialButtons = Object.keys(CREDENTIAL_TYPES).map(index => (
    <CustomButton
      buttonProps={{
        onClick: () => changeCurrentCredential(parseInt(index, 10)),
        className: 'theme-list',
        disabled: parseInt(index, 10) !== availableCredential
      }}
      img={indexToImgTrad[index]}
      buttonText={t(`credential.${CREDENTIAL_TYPES[index]}`)}
      key={index}
      optImg={
        showCongrats || showContactButton || parseInt(index, 10) < availableCredential
          ? { src: checkIcon, alt: 'Verified Icon' }
          : {}
      }
    />
  ));

  return (
    <div className="CredentialListContainer">
      {credentialButtons}
      {showContactButton && <ContactButton toContactForm={toContactForm} />}
    </div>
  );
};

CredentialsList.propTypes = {
  changeCurrentCredential: PropTypes.func.isRequired,
  availableCredential: PropTypes.number.isRequired,
  showContactButton: PropTypes.bool.isRequired,
  showCongrats: PropTypes.bool.isRequired,
  toContactForm: PropTypes.func.isRequired
};

export default CredentialsList;
