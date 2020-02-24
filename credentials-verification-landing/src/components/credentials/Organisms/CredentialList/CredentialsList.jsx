import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { CREDENTIAL_TYPES } from '../../../../helpers/constants';

import './_style.scss';

const indexToImgTrad = {
  0: { src: 'images/icon-credential-id.svg', alt: 'Id Credential Id' },
  1: { src: 'images/icon-credential-university.svg', alt: 'University Credential' },
  2: { src: 'images/icon-credential-employment.svg', alt: 'Employment Credential' },
  3: { src: 'images/icon-credential-insurance.svg', alt: 'Insurance Credential' }
};

const CredentialsList = ({ changeCurrentCredential, availableCredential }) => {
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
    />
  ));

  return <div className="CredentialListContainer"> {credentialButtons} </div>;
};

CredentialsList.propTypes = {
  changeCurrentCredential: PropTypes.func.isRequired,
  availableCredential: PropTypes.number.isRequired
};

export default CredentialsList;
