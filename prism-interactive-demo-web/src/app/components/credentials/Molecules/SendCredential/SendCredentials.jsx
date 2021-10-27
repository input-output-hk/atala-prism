import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import employmentIcon from '../../../../images/shared-proof.png';
import universityIcon from '../../../../images/shared-university.png';
import insuranceIcon from '../../../../images/shared-insurance.png';
import {
  UNIVERSITY_DEGREE,
  PROOF_OF_EMPLOYMENT,
  INSURANCE_POLICY
} from '../../../../../helpers/constants';

const shareIcon = {
  [UNIVERSITY_DEGREE]: universityIcon,
  [PROOF_OF_EMPLOYMENT]: employmentIcon,
  [INSURANCE_POLICY]: insuranceIcon
};

const SendCredentials = ({ currentCredential }) => {
  const { t } = useTranslation();

  return (
    <div className="FinishInfo">
      <h1>
        <strong>{t('credential.sendCredentials.title')}</strong>
      </h1>
      <h3>
        {t('credential.sendCredentials.explanation')}
        {t(`credential.sendCredentials.${currentCredential}`)},
        {t('credential.sendCredentials.explanation2')}
        {currentCredential > 1 &&
          t('credential.sendCredentials.explanation3') +
            t(`credential.sendCredentials.${currentCredential - 1}`)}
        .
      </h3>
      <div className="FinishContent">
        <div className="IntroTutorial">
          <img
            src={shareIcon[currentCredential]}
            alt={t('credential.sendCredentials.sharedIconAlt')}
          />
        </div>
        <div className="StepTextItem">
          <p className="NumberText">1.</p>
          <p>{t('credential.sendCredentials.share')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">2.</p>
          <p>{t('credential.sendCredentials.governmentCredentialId')}</p>
        </div>
      </div>
    </div>
  );
};

SendCredentials.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default SendCredentials;
