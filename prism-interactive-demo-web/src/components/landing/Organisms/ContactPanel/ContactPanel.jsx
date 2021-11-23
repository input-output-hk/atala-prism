import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';
import Contact from '../../../../app/components/contact/Organisms/Contact/Contact';

const ContactPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="ContactPanel">
      <div className="ContactPanelContainer">
        <div className="paragraphContent">
          <h1>{t('landing.contactPanel.title')}</h1>
          <h3>
            {t('landing.contactPanel.part1')}{' '}
            <a href="https://www.cardano.org/" target="_blank" rel="noopener noreferrer">
              {t('landing.contactPanel.part2')}
            </a>
            {t('landing.contactPanel.part3')}
          </h3>
          <h3>
            {t('landing.contactPanel.part4')}{' '}
            <a href="https://iohk.io/" target="_blank" rel="noopener noreferrer">
              {t('landing.contactPanel.part5')}
            </a>{' '}
            {t('landing.contactPanel.part6')}
          </h3>
          <h3>{t('landing.contactPanel.part7')}</h3>
        </div>
      </div>
      <div className="ContactPanelForm">
        <Contact />
      </div>
    </div>
  );
};

export default ContactPanel;
