import React, { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import Header from '../common/Molecules/Header/Header';
import IntroSection from './Organisms/IntroSection/IntroSection';
import CredentialSection from './Organisms/CredentialSection/CredentialSection';
import FindCredential from './Molecules/FindCredential/FindCredential';
import './_style.scss';
import Footer from '../common/Molecules/Footer/Footer';
import { scrollToRef } from '../../helpers/genericHelpers';
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
} from '../../helpers/constants';
import TrustSection from './Organisms/TrustSection/TrustSection';
import QuotesPanel from './Molecules/QuotesPanel/QuotesPanel';
import CaseStudy from './Molecules/CaseStudy/CaseStudy';
import GetStarted from './Molecules/GetStarted/GetStarted';
import FaqPanel from './Molecules/FaqPanel/FaqPanel';
import ContactPanel from './Organisms/ContactPanel/ContactPanel';
import UseCasesPanel from './Organisms/UseCasesPanel/UseCasesPanel';
import { useState } from 'react';
import SupportButton from '../common/Atoms/SupportButton/SupportButton';

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

  const refTranslator = {
    [VISION_NAME]: visionSection,
    [DEMO_NAME]: demoSection,
    [COMPONENTS_NAME]: componentsSection,
    [BENEFITS_NAME]: benefitsSection,
    [USE_CASES_NAME]: useCasesSection,
    [CASE_STUDY_NAME]: caseStudySection,
    [GET_STARTED_NAME]: getStartedSection,
    [FAQ_NAME]: faqSection,
    [CONTACT_US_NAME]: contactUsSection
  };

  const executeScroll = ref => scrollToRef(refTranslator[ref]);

  return (
    <div className="LandingContainer">
      <div className="LandingHeader">
        <Header
          executeScroll={executeScroll}
          currentSection={currentSection}
          setCurrent={key => setCurrentSection(key)}
        />
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
      <div ref={visionSection} onMouseOver={() => setCurrentSection(VISION_NAME)}>
        <IntroSection />
        <QuotesPanel />
      </div>

      <div ref={demoSection} onMouseOver={() => setCurrentSection(DEMO_NAME)}>
        <FindCredential />
      </div>

      <div ref={componentsSection} onMouseOver={() => setCurrentSection(COMPONENTS_NAME)}>
        <CredentialSection />
      </div>
      <div ref={benefitsSection} onMouseOver={() => setCurrentSection(BENEFITS_NAME)}>
        <TrustSection />
      </div>
      <div ref={useCasesSection} onMouseOver={() => setCurrentSection(USE_CASES_NAME)}>
        <UseCasesPanel />
      </div>
      <div ref={caseStudySection} onMouseOver={() => setCurrentSection(CASE_STUDY_NAME)}>
        <CaseStudy />
      </div>
      <div ref={getStartedSection} onMouseOver={() => setCurrentSection(GET_STARTED_NAME)}>
        <GetStarted executeScroll={executeScroll} />
      </div>
      <div ref={faqSection} onMouseOver={() => setCurrentSection(FAQ_NAME)}>
        <FaqPanel />
      </div>
      <div ref={contactUsSection} onMouseOver={() => setCurrentSection(CONTACT_US_NAME)}>
        <ContactPanel />
      </div>
      <Footer executeScroll={executeScroll} />

      <SupportButton />
    </div>
  );
};

export default Landing;
