import React from 'react';
import { useTranslation } from 'react-i18next';
import { scrollToTop } from '../../../../helpers/genericHelpers';
import NavBar from '../NavBar/NavBar';
import './_style.scss';

const Header = props => {
  const { t } = useTranslation();

  return (
    <div className="Header">
      <div className="Logo">
        <button type="button" onClick={scrollToTop}>
          <img src="images/atala-prism-logo-suite.svg" alt={t('atalaLogo')} />
        </button>
      </div>
      <div className="NavBar">
        <NavBar {...props} />
      </div>
    </div>
  );
};

export default Header;
