import React from 'react';
import { useTranslation } from 'react-i18next';

import './_style.scss';
import Contact from '../../../contact/Organisms/Contact/Contact';

const ContactPanel = () => {
  const { t } = useTranslation();

  return (
    <div className="ContactPanel">
      <div className="ContactPanelContainer">
        <span className="MiniDetailText">{t('landing.contactPanel.detailText')}</span>
        <div className="paragraphContent">
          <h1>{t('landing.contactPanel.title')}</h1>
          <h3>
            {t('landing.contactPanel.part1')} <a>{t('landing.contactPanel.part2')}</a>
            {t('landing.contactPanel.part3')}
          </h3>
          <h3>
            {t('landing.contactPanel.part4')} <a>{t('landing.contactPanel.part5')}</a>{' '}
            {t('landing.contactPanel.part6')}
          </h3>
          <h3>{t('landing.contactPanel.part7')}</h3>
        </div>
      </div>
      <div className="ContactPanelForm">
        <Contact/>
      </div>
      <img src="images/illustration-footer.svg" alt={t('atalaLogo')} className="IllustrationFooter" />
    </div>
  );
};

export default ContactPanel;
