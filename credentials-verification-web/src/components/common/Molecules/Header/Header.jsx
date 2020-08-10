import React from 'react';
import { Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import LanguageSelector from '../LanguageSelector/LanguageSelector';
import { getThemeByRole } from '../../../../helpers/themeHelper';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const Header = () => {
  const { t } = useTranslation();
  const { session } = useSession();

  const userLogo = getLogoAsBase64(session.logo);

  const theme = getThemeByRole(session.userRole);

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
        <UserAvatar />
        <LanguageSelector />
      </Col>
    </Row>
  );
};

export default Header;
