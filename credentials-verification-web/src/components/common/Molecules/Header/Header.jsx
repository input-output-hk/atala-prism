import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'antd';
import './_style.scss';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { withApi } from '../../../providers/withApi';
import { getLogoAsBase64 } from '../../../../helpers/genericHelpers';
import LanguageSelector from '../LanguageSelector/LanguageSelector';

const Header = ({ api: { lockWallet } }) => {
  const userLogo = getLogoAsBase64();

  return (
    <Row type="flex" align="middle" className="HeaderContainer">
      <Col xs={10} sm={12} md={12} lg={12}>
        <a href="/">
          <img className="logo" src="atala-logo.svg" alt="Atala Logo" />
        </a>
      </Col>
      <Col
        xs={14}
        sm={{ span: 7, offset: 5 }}
        md={{ span: 6, offset: 6 }}
        lg={{ span: 6, offset: 6 }}
      >
        <Col xs={6} sm={6} md={9} lg={3}>
          <img
            className="IconUniversity"
            src="icon-free-university.svg"
            alt="Free University Tbilisi"
          />
        </Col>
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
