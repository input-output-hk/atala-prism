import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import PropTypes from 'prop-types';
import firebase from 'gatsby-plugin-firebase';
import InteractiveMap from '../../../../app/components/common/Organisms/InteractiveMap/InteractiveMap';
import CustomButton from '../../../customButton/CustomButton';
import { withRedirector } from '../../../../app/components/providers/withRedirector';
import DownloadButtons from '../DownloadButtons/DownloadButtons';
import { GET_CREDENTIALS_EVENT } from '../../../../helpers/constants';

import './_style.scss';

const FindCredential = ({ redirector: { redirectToCredentials }, isTesting }) => {
  const { t } = useTranslation();

  const isSsr = typeof window === 'undefined';

  const startDemo = () => {
    firebase.analytics().logEvent(GET_CREDENTIALS_EVENT);
    redirectToCredentials();
  };

  return (
    <div className="FindCredential">
      <div className="TextContainer">
        <div className="InteractiveDemoText">
          <h1>{t('landing.findCredential.title')}</h1>
          <h1>{t('landing.findCredential.title2')}</h1>
          <h1>{t('landing.findCredential.title3')}</h1>
          <div className="DescriptionText">
            <p>{t('landing.findCredential.descriptionText')}</p>
            <h3>{t('landing.findCredential.subtitle')}</h3>
          </div>
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
        </div>
      </div>
      {!isTesting && !isSsr && <InteractiveMap controlsEnabled={false} />}
    </div>
  );
};

FindCredential.defaultProps = {
  isTesting: false
};

FindCredential.propTypes = {
  redirector: PropTypes.shape({ redirectToCredentials: PropTypes.func }).isRequired,
  isTesting: PropTypes.bool
};

export default withRedirector(FindCredential);
