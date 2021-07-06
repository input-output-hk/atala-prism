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
          <img src="images/atala-prism-logo-suite.svg" alt={t('atalaLogo')} />
        </button>
        <div className="FooterText">
          <p>
            <a
              href="https://legal.atalaprism.io/terms-and-conditions.html "
              target="_blank"
              rel="noopener noreferrer"
            >
              {t('landing.trademark.part1')}
            </a>
          </p>
          <p>
            <a
              href="https://legal.atalaprism.io/privacy-policy.html "
              target="_blank"
              rel="noopener noreferrer"
            >
              {t('landing.trademark.part2')}
            </a>
          </p>
        </div>
        <p>{t('landing.trademark.part3')}</p>
      </div>
    </div>
  );
};

export default Footer;
