import React, { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import Header from '../common/Molecules/Header/Header';
import IntroSection from './Organisms/IntroSection/IntroSection';
import CredentialSection from './Organisms/CredentialSection/CredentialSection';
import FindCredential from './Organisms/FindCredential/FindCredentail';
import DownloadButtons from './Molecules/DownloadButtons/DownloadButtons';
import './_style.scss';
import Footer from '../common/Molecules/Footer/Footer';
import { scrollToRef } from '../../helpers/genericHelpers';
import { FEATURE_NAME, CREDENTIAL_NAME, DOWNLOAD_NAME } from '../../helpers/constants';
import TrustSection from './Organisms/TrustSection/TrustSection';

const Landing = () => {
  const { t } = useTranslation();

  const featuresSection = useRef(null);
  const credentialsSection = useRef(null);
  const downloadSection = useRef(null);

  const refTranslator = {
    [FEATURE_NAME]: featuresSection,
    [CREDENTIAL_NAME]: credentialsSection,
    [DOWNLOAD_NAME]: downloadSection
  };

  const executeScroll = ref => scrollToRef(refTranslator[ref]);

  return (
    <div className="LandingContainer">
      <div className="LandingHeader">
        <Header executeScroll={executeScroll} />
        <div className="LadingHeaderContent">
          <div className="HeaderText">
            <h1>{t('landing.start.info')}</h1>
            <h3>{t('landing.start.subtitle')}</h3>
            <DownloadButtons />
          </div>
          <div className="HeaderImages">
            <img src="images/phones-header.png" alt={t('landing.downloadAndroidAlt')} />
          </div>
        </div>
      </div>
      <div ref={featuresSection}>
        <IntroSection />
      </div>
      <div ref={credentialsSection}>
        <CredentialSection />
      </div>
      <FindCredential />
      <div ref={downloadSection}>
        <TrustSection />
      </div>
      <Footer executeScroll={executeScroll} />
    </div>
  );
};

export default Landing;
