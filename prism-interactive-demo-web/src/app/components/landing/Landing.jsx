import React, { useRef } from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import Header from '../../../components/Header/Header';
import IntroSection from './Organisms/IntroSection/IntroSection';
import CredentialSection from './Organisms/CredentialSection/CredentialSection';
import FindCredential from './Molecules/FindCredential/FindCredential';
import Footer from '../common/Molecules/Footer/Footer';
import {
  VISION_NAME,
  DEMO_NAME,
  COMPONENTS_NAME,
  BENEFITS_NAME,
  USE_CASES_NAME,
  CASE_STUDY_NAME,
  GET_STARTED_NAME,
  FAQ_NAME,
  CONTACT_US_NAME
} from '../../../helpers/constants';
import TrustSection from './Organisms/TrustSection/TrustSection';
import QuotesPanel from './Molecules/QuotesPanel/QuotesPanel';
import CaseStudy from './Molecules/CaseStudy/CaseStudy';
import GetStarted from './Molecules/GetStarted/GetStarted';
import FaqPanel from './Molecules/FaqPanel/FaqPanel';
import ContactPanel from './Organisms/ContactPanel/ContactPanel';
import UseCasesPanel from './Organisms/UseCasesPanel/UseCasesPanel';
import { useState } from 'react';
import SupportButton from '../common/Atoms/SupportButton/SupportButton';

import './_style.scss';

const Landing = () => {
  const { t } = useTranslation();

  const [currentSection, setCurrentSection] = useState(null);

  const visionSection = useRef(null);
  const demoSection = useRef(null);
  const componentsSection = useRef(null);
  const benefitsSection = useRef(null);
  const useCasesSection = useRef(null);
  const caseStudySection = useRef(null);
  const getStartedSection = useRef(null);
  const faqSection = useRef(null);
  const contactUsSection = useRef(null);

  return (
    <div className="LandingContainer">
      <div className="LandingHeader">
        <Header currentSection={currentSection} />
        <div className="LadingHeaderContent" onMouseOver={() => setCurrentSection(null)}>
          <div className="HeaderText">
            <h1>{t('landing.start.info')}</h1>
            <h3>{t('landing.start.subtitle')}</h3>
          </div>
          <div className="HeaderImages">
            <img src="/images/atala-prism-graph.svg" alt={t('landing.downloadAndroidAlt')} />
          </div>
        </div>
      </div>
      <div id={VISION_NAME} ref={visionSection} onMouseOver={() => setCurrentSection(VISION_NAME)}>
        <IntroSection />
        <QuotesPanel />
      </div>
      <div id={DEMO_NAME} ref={demoSection} onMouseOver={() => setCurrentSection(DEMO_NAME)}>
        <FindCredential />
      </div>
      <div
        id={COMPONENTS_NAME}
        ref={componentsSection}
        onMouseOver={() => setCurrentSection(COMPONENTS_NAME)}
      >
        <CredentialSection />
      </div>
      <div
        id={BENEFITS_NAME}
        ref={benefitsSection}
        onMouseOver={() => setCurrentSection(BENEFITS_NAME)}
      >
        <TrustSection />
      </div>
      <div
        id={USE_CASES_NAME}
        ref={useCasesSection}
        onMouseOver={() => setCurrentSection(USE_CASES_NAME)}
      >
        <UseCasesPanel />
      </div>
      <div
        id={CASE_STUDY_NAME}
        ref={caseStudySection}
        onMouseOver={() => setCurrentSection(CASE_STUDY_NAME)}
      >
        <CaseStudy />
      </div>
      <div
        id={GET_STARTED_NAME}
        ref={getStartedSection}
        onMouseOver={() => setCurrentSection(GET_STARTED_NAME)}
      >
        <GetStarted />
      </div>
      <div id={FAQ_NAME} ref={faqSection} onMouseOver={() => setCurrentSection(FAQ_NAME)}>
        <FaqPanel />
      </div>
      <div
        id={CONTACT_US_NAME}
        ref={contactUsSection}
        onMouseOver={() => setCurrentSection(CONTACT_US_NAME)}
      >
        <ContactPanel />
      </div>
      <Footer />
      <SupportButton />
    </div>
  );
};

export default Landing;
