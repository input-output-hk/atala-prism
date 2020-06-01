import React, { useEffect, useContext } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { useAnalytics } from 'reactfire';
import InteractiveMap from '../../../common/Organisms/InteractiveMap/InteractiveMap';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';
import DownloadButtons from '../DownloadButtons/DownloadButtons';
import { GET_CREDENTIALS } from '../../../../helpers/constants';

import './_style.scss';

const FindCredential = ({ redirector: { redirectToCredentials } }) => {
  const firebase = useAnalytics();
  const { t } = useTranslation();


  const startDemo = () => {
    firebase.logEvent(GET_CREDENTIALS);
    redirectToCredentials();
  };

  return (
    <div className="FindCredential">
      <div className="TextContainer">
        <span className="MiniDetailText">{t('landing.findCredential.detailText')}</span>
        <div className="InteractiveDemoText">
          <h1>{t('landing.findCredential.title')}</h1>
          <h1>{t('landing.findCredential.title2')}</h1>
          <h3>{t('landing.findCredential.subtitle')}</h3>
          <div className="StepTextItem">
            <p className="NumberText">Step 1 .</p>
            <p>{t('landing.findCredential.step1')}</p>
          </div>
          <DownloadButtons />
          <div className="StepTextItem">
            <p className="NumberText">Step 2 .</p>
            <p>{t('landing.findCredential.step2')}</p>
          </div>
          <CustomButton
            buttonProps={{ className: 'theme-primary', onClick: startDemo }}
            buttonText={t('landing.findCredential.askForCredential')}
          />
          <div className="DisclaimerText">
            <p>{t('landing.findCredential.disclaimer')}</p>
          </div>
        </div>
      </div>
      <InteractiveMap />
    </div>
  );
};

FindCredential.propTypes = {
  redirector: PropTypes.shape({ redirectToCredentials: PropTypes.func }).isRequired
};

export default withRedirector(FindCredential);
