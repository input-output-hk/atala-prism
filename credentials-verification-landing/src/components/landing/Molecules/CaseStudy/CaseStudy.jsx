import React from 'react';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const CaseStudy = () => {
  const { t } = useTranslation();

  return (
    <div className="CaseStudyContent">
      <div className="TextContainer">
        <span className="MiniDetailText">{t('landing.caseStudy.detailText')}</span>
        <div className="CaseStudyDescription">
        <h1>{t('landing.caseStudy.title')}</h1>
        <h1>{t('landing.caseStudy.title2')}</h1>
        <p>{t('landing.caseStudy.part1')}</p>
        <p>{t('landing.caseStudy.part2')}</p>
        </div>
      </div>
      <div className="ImageContainer">
        <img src="images/case-study-georgia.png" alt={t('landing.caseStudy.caseStudyAlt')} />
      </div>
    </div>
  );
};

export default CaseStudy;
