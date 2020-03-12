import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { INSURANCE_POLICY } from '../../../../helpers/constants';
import idIcon from '../../../../images/credential-id-phone.png';
import employmentIcon from '../../../../images/credential-employment-phone.png';
import universityIcon from '../../../../images/credential-university-phone.png';
import insuranceIcon from '../../../../images/credential-insurance-phone.png';
import homeAppIcon from '../../../../images/icon-app-home.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const finishedIcon = {
  0: idIcon,
  1: universityIcon,
  2: employmentIcon,
  3: insuranceIcon
};

const FinishInfo = ({ confirmSuccessCredential, currentCredential }) => {
  const { t } = useTranslation();

  return (
    <div className="FinishInfo">
      <h1>
        <strong>{t('credential.finishInfo.congrats')}</strong>
      </h1>
      <h1>{t('credential.finishInfo.title')}</h1>
      <h3>{t('credential.finishInfo.explanation')}</h3>
      <div className="FinishContent">
        <div className="IntroTutorial">
          <img
            src={finishedIcon[currentCredential]}
            alt={t('credential.finishInfo.finishedIconAlt')}
          />
        </div>
        <div className="StepTextItem">
          <p className="NumberText">1.</p>
          <p>{t('credential.finishInfo.openOnPhone')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">2.</p>
          <p>{t('credential.finishInfo.checkHome')}</p>
          <img src={homeAppIcon} alt="Home Icon" />
          <p>{t('credential.finishInfo.checkHome2')}</p>
        </div>
        <div className="StepTextItem">
          <p>
            <strong>{t(`credential.credentialNames.CredentialType${currentCredential}`)}.</strong>
          </p>
        </div>
        {currentCredential !== INSURANCE_POLICY && (
          <div className="centeredButton">
            <CustomButton
              buttonProps={{ onClick: confirmSuccessCredential, className: 'theme-secondary' }}
              buttonText={t('credential.finishInfo.finished')}
            />
          </div>
        )}
      </div>
    </div>
  );
};

FinishInfo.propTypes = {
  confirmSuccessCredential: PropTypes.func.isRequired,
  currentCredential: PropTypes.number.isRequired
};

export default FinishInfo;
