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
      <h1>
        <strong>{t('credential.finishInfo.title')}</strong>
      </h1>
      <Row>
        <label>{t('credential.finishInfo.explanation')}</label>
      </Row>
      <Row>
        <img src={finishedIcon} alt={t('credential.finishInfo.finishedIconAlt')} />
      </Row>
      <Row>
        <p>
          {t('credential.finishInfo.howToSee')}
          <strong>{t('credential.finishInfo.whatToSee')}</strong>
        </p>
      </Row>
      <br />
      <div className="centeredButton">
        <CustomButton
          buttonProps={{ onClick: redirectToLanding, className: 'theme-secondary' }}
          buttonText={t('credential.finishInfo.finished')}
        />
      </div>
    </div>
  );
};

FinishInfo.propTypes = {
  redirector: PropTypes.shape({ redirectToLanding: PropTypes.func }).isRequired
};

export default withRedirector(FinishInfo);
