import React from 'react';
import { Link } from 'gatsby';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const Footer = () => {
  const { t } = useTranslation();

  return (
    <div className="Footer">
      <div className="FooterContent">
        <Link to="/">
          <img src="/images/atala-prism-logo-suite.svg" alt={t('atalaLogo')} />
        </Link>
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
