import React from 'react';
import { useTranslation } from 'react-i18next';
import { scrollToTop } from '../../../../helpers/genericHelpers';

import './_style.scss';

const Footer = () => {
  const { t } = useTranslation();

  return (
    <div className="Footer">
      <div className="FooterContent">
        <button type="button" onClick={scrollToTop}>
          <img src="images/atala-prism-black.svg" alt={t('atalaLogo')} />
        </button>
        <div className="FooterText">
          <p>{t('landing.trademark.part1')}</p>
          <p>{t('landing.trademark.part2')}</p>
        </div>
        <p>{t('landing.trademark.part3')}</p>
      </div>
    </div>
  );
};

export default Footer;
