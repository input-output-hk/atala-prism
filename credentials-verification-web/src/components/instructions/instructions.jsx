import React from 'react';
import { useTranslation } from 'react-i18next';
import imgLogo from '../../images/logo-instructions.svg';
import imgScreenshots from '../../images/img-instructions.png';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import './style.scss';

const Instructions = ({ name, bundle, credentials, proofRequests }) => {
  const { t } = useTranslation();

  return (
    <>
      <div className="instructionsHeader">
        <img src={imgLogo} alt="" />
      </div>
      <div className="instructionsWrapper">
        <div className="instructionsText">
          <h4>{t('instructions.instructions')}</h4>
          <h1>{t('instructions.title')}</h1>
          <p>
            {t('instructions.the')}
            <span>{t('instructions.atalaPrism')}</span>
            {t('instructions.description')}
            <span>{t('instructions.chrome')}</span>
            {t('instructions.descriptionPartTwo')}
          </p>
          <div>
            <CustomButton
              buttonProps={{ className: 'theme-outline' }}
              buttonText={t('instructions.button')}
            />
          </div>
          <h3 className="italic">
            {t('instructions.comingSoon')}
            <span>{t('instructions.chromeWeb')}</span>
          </h3>
          <h5 className="register-text">
            {t('instructions.register')}
            <span>{t('instructions.clickRegister')}</span>
          </h5>
        </div>

        <div className="instructionsImg">
          <img src={imgScreenshots} alt="instructionsImg" />
        </div>
      </div>
    </>
  );
};

export default Instructions;
