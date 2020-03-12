import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import idIcon from '../../../../images/government-id-details.png';
import employmentIcon from '../../../../images/shared-proof.png';
import universityIcon from '../../../../images/shared-university.png';
import insuranceIcon from '../../../../images/shared-insurance.png';
import shareAppIcon from '../../../../images/icon-share.svg';
import homeAppIcon from '../../../../images/icon-app-home.svg';
import {
  UNIVERSITY_DEGREE,
  PROOF_OF_EMPLOYMENT,
  INSURANCE_POLICY
} from '../../../../helpers/constants';

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
        {t(`credential.sendCredentials.${currentCredential}`)}.
        {t('credential.sendCredentials.explanation2')}
        {currentCredential > 1 &&
          t('credential.sendCredentials.explanation3') +
            t(`credential.sendCredentials.${currentCredential - 1}`)}
        .
      </h3>
      <div className="FinishContent">
        <div className="IntroTutorial">
          <img src={idIcon} alt={t('credential.sendCredentials.credentialIconAlt')} />
          <img
            src={shareIcon[currentCredential]}
            alt={t('credential.sendCredentials.sharedIconAlt')}
          />
        </div>
        <div className="StepTextItem">
          <p className="NumberText">1.</p>
          <p>{t('credential.sendCredentials.openOnPhone')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">2.</p>
          <p>{t('credential.sendCredentials.checkHome')}</p>
          <img src={homeAppIcon} alt="Home Icon" />
          <p>{t('credential.sendCredentials.checkHome2')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">3.</p>
          <p>{t('credential.sendCredentials.openCredentialID')}</p>
          <p>
            <strong>{t('credential.sendCredentials.governmentCredentialId')}</strong>
          </p>
          <p>{t('credential.sendCredentials.openCredentialID2')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">4.</p>
          <p>{t('credential.sendCredentials.share')}</p>
          <img src={shareAppIcon} alt="Share Icon" />
          <p>{t('credential.sendCredentials.share2')}</p>
        </div>
        {currentCredential > 1 && (
          <div className="StepTextItem">
            <p className="NumberText">5.</p>
            <p>{t('credential.sendCredentials.optionalOpen')}</p>
            <p>
              <strong>{t(`credential.sendCredentials.${currentCredential - 1}`)}</strong>.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

SendCredentials.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default SendCredentials;
