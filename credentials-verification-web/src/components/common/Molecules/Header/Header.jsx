import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { withApi } from '../../../providers/withApi';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import LanguageSelector from '../LanguageSelector/LanguageSelector';
import { theme } from '../../../../helpers/themeHelper';

import './_style.scss';

const Header = ({ api: { wallet } }) => {
  const userLogo = getLogoAsBase64();
  const { t } = useTranslation();

  // This wrapper is necessary to preserve lockWallet context
  const lockWallet = async () => wallet.lockWallet();

  return (
    <Row type="flex" align="middle" className={`HeaderContainer ${theme.class()}`}>
      <Col lg={8} className="LogoContainer">
        <a href="/">
          <img className="HeaderLogo" src="atala-logo.svg" alt="Atala Logo" />
        </a>
        <div className="PortalName">
          <p>{t(theme.title())}</p>
        </div>
      </Col>
      <Col lg={16} className="RightSide">
        <img
          className="IconUniversity"
          src={userLogo || 'icon-free-university.svg'}
          alt="Free University Tbilisi"
        />
        <UserAvatar lockWallet={lockWallet} />
        <LanguageSelector />
      </Col>
    </Row>
  );
};

Header.propTypes = {
  api: PropTypes.shape({
    wallet: PropTypes.shape({
      lockWallet: PropTypes.func.isRequired
    }).isRequired
  }).isRequired
};

export default withApi(Header);
