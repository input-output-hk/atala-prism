import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import finishedIcon from '../../../../images/credential-id-phone.png';
import homeAppIcon from '../../../../images/icon-app-home.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';

import './_style.scss';

const FinishInfo = ({ redirector: { redirectToLanding } }) => {
  const { t } = useTranslation();

  return (
    <div className="FinishInfo">
      <h1>
        <strong>{t('credential.finishInfo.congrats')}</strong>
      </h1>
      <h1>{t('credential.finishInfo.title')}</h1>
      <h3>{t('credential.finishInfo.explanation')}</h3>
      <div className="FinishContent">
        <img src={finishedIcon} alt={t('credential.finishInfo.finishedIconAlt')} />
        <div className="StepTextItem">
          <p className="NumberText">1.</p>
          <p>{t('credential.finishInfo.openOnPhone')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">2.</p>
          <p>{t('credential.finishInfo.checkHome')}</p>
          <img src={homeAppIcon} alt="Home Icon" />
          <p>{t('credential.finishInfo.IdType1')}</p>
        </div>
        <div className="StepTextItem">
          <p>{t('credential.finishInfo.IdType2')}</p>
        </div>
        <div className="centeredButton">
          <CustomButton
            buttonProps={{ onClick: redirectToLanding, className: 'theme-secondary' }}
            buttonText={t('credential.finishInfo.finished')}
          />
        </div>
      </div>
    </div>
  );
};

FinishInfo.propTypes = {
  redirector: PropTypes.shape({ redirectToLanding: PropTypes.func }).isRequired
};

export default withRedirector(FinishInfo);
