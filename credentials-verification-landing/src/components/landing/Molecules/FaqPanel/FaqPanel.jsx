import React from 'react';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const FaqPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="FaqPanelContent">
      <div className="TextContainer">
        <h1>{t('landing.faqPanel.title')}</h1>
        <div className="FaqContainer">
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq1')}</h3>
            <p>{t('landing.faqPanel.faq1')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq2')}</h3>
            <p>{t('landing.faqPanel.faq2')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq3')}</h3>
            <p>{t('landing.faqPanel.faq3')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq4')}</h3>
            <p>{t('landing.faqPanel.faq4')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq5')}</h3>
            <p>{t('landing.faqPanel.faq5')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq6')}</h3>
            <p>{t('landing.faqPanel.faq6')}</p>
          </div>
          <div className="FaqItem">
            <h3>{t('landing.faqPanel.titlefaq7')}</h3>
            <p>{t('landing.faqPanel.faq7')}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FaqPanel;
