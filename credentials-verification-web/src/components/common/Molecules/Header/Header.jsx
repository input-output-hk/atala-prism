import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'antd';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { withApi } from '../../../providers/withApi';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import LanguageSelector from '../LanguageSelector/LanguageSelector';

import './_style.scss';

const Header = ({ api: { lockWallet } }) => {
  const userLogo = getLogoAsBase64();

  return (
    <Row type="flex" align="middle" className="HeaderContainer">
      <Col lg={8}>
        <a href="/">
          <img className="HeaderLogo" src="atala-logo.svg" alt="Atala Logo" />
        </a>
      </Col>
      <Col
        lg={16}
        className="RightSide"
        // xs={14}
        // sm={{ span: 7, offset: 5 }}
        // md={{ span: 6, offset: 6 }}
        // lg={{ span: 6, offset: 6 }}
      >
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
    lockWallet: PropTypes.func
  }).isRequired
};

export default withApi(Header);
