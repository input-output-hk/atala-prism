import React from 'react';
import { Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import LanguageSelector from '../LanguageSelector/LanguageSelector';
import { useSession } from '../../../../hooks/useSession';

import './_style.scss';

const Header = observer(() => {
  const { t } = useTranslation();
  const { session } = useSession();

  const userLogo = session.logo ? getLogoAsBase64(session.logo) : null;

  return (
    <Row type="flex" align="middle" className="HeaderContainer">
      <Col lg={8} className="LogoContainer">
        <a href="/">
          <img className="HeaderLogo" src="/atala_new_logo.svg" alt="Atala Logo" />
        </a>
        <div className="PortalName">
          <p>{t('theme.title')}</p>
        </div>
      </Col>
      <Col lg={16} className="RightSide">
        <UserAvatar logo={userLogo} />
        <LanguageSelector />
      </Col>
    </Row>
  );
});

export default Header;
