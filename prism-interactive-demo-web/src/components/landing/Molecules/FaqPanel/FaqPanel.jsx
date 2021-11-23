import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const faqs = [
  { title: 'landing.faqPanel.titlefaq1', description: 'landing.faqPanel.faq1' },
  { title: 'landing.faqPanel.titlefaq2', description: 'landing.faqPanel.faq2' },
  { title: 'landing.faqPanel.titlefaq3', description: 'landing.faqPanel.faq3' },
  { title: 'landing.faqPanel.titlefaq4', description: 'landing.faqPanel.faq4' },
  { title: 'landing.faqPanel.titlefaq5', description: 'landing.faqPanel.faq5' },
  { title: 'landing.faqPanel.titlefaq6', description: 'landing.faqPanel.faq6' },
  { title: 'landing.faqPanel.titlefaq7', description: 'landing.faqPanel.faq7' },
  { title: 'landing.faqPanel.titlefaq8', description: 'landing.faqPanel.faq8' },
  { title: 'landing.faqPanel.titlefaq9', description: 'landing.faqPanel.faq9' },
  { title: 'landing.faqPanel.titlefaq10', description: 'landing.faqPanel.faq10' }
];

const FaqPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="FaqPanelContent">
      <div className="TextContainer">
        <h1>{t('landing.faqPanel.title')}</h1>
        <div className="FaqContainer">
          {faqs.map(({ title, description }) => (
            <div className="FaqItem">
              <h3>{t(title)}</h3>
              <p>{t(description)}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default FaqPanel;
