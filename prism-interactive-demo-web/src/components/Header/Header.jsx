import React from 'react';
import { Link } from 'gatsby';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import NavBar from '../NavBar/NavBar';

import './_style.scss';

const Header = props => {
  const { t } = useTranslation();

  return (
    <div className="Header">
      <div className="Logo">
        <Link to="/">
          <img src="/images/atala-prism-logo-suite.svg" alt={t('atalaLogo')} />
        </Link>
      </div>
      <div className="NavBar">
        <NavBar {...props} />
      </div>
    </div>
  );
};

export default Header;
