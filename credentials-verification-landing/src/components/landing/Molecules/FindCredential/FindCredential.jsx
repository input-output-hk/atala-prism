import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import InteractiveMap from '../../../common/Organisms/InteractiveMap/InteractiveMap';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';

import './_style.scss';
import DownloadButtons from '../DownloadButtons/DownloadButtons';

const FindCredential = ({ redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();

  return (
    <div className="FindCredential">
      <div className="TextContainer">
        <span className="MiniDetailText">{t('landing.findCredential.detailText')}</span>
        <div className="InteractiveDemoText">
          <h1>{t('landing.findCredential.title')}</h1>
          <h1>{t('landing.findCredential.title2')}</h1>
          <h3>{t('landing.findCredential.subtitle')}</h3>
          <div className="StepTextItem">
            <p className="NumberText">01.</p>
            <p>{t('landing.findCredential.step1')}</p>
          </div>
          <DownloadButtons />
          <div className="StepTextItem">
            <p className="NumberText">02.</p>
            <p>{t('landing.findCredential.step2')}</p>
          </div>
          <div className="StepTextItem">
            <p className="NumberText">03.</p>
            <p>{t('landing.findCredential.step3')}</p>
          </div>
          <CustomButton
            buttonProps={{ className: 'theme-primary', onClick: redirectToCredentials }}
            buttonText={t('landing.findCredential.askForCredential')}
          />
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
