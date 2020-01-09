import React from 'react';
import { useTranslation } from 'react-i18next';
import { scrollToTop } from '../../../../helpers/genericHelpers';

import './_style.scss';

const Footer = () => {
  const { t } = useTranslation();

  return (
    <div className="Footer">
      <button type="button" onClick={scrollToTop}>
        <img src="images/atala-logo.svg" alt={t('atalaLogo')} />
      </button>
      <div className="FooterText">
        <p>
          {t('landing.trademark.part1')}
          {t('landing.trademark.part2')}
        </p>
      </div>
    </div>
  );
};

export default Footer;
