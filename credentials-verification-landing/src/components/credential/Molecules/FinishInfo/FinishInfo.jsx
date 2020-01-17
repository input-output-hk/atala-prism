import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Row } from 'antd';
import finishedIcon from '../../../../images/finished.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';

import './_style.scss';

const FinishInfo = ({ redirector: { redirectToLanding } }) => {
  const { t } = useTranslation();

  return (
    <div className="FinishInfo">
      <h1>{t('credential.finishInfo.title')}</h1>
      <h3>{t('credential.finishInfo.explanation')}</h3>
      <div className="FinishContent">
        <img src={finishedIcon} alt={t('credential.finishInfo.finishedIconAlt')} />
        <p>
          {t('credential.finishInfo.howToSee')}
          <strong>{t('credential.finishInfo.whatToSee')}</strong>
        </p>
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
