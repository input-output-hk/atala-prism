import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'antd';
import './_style.scss';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';
import { withApi } from '../../../providers/withApi';

const Header = ({ api: { lockWallet } }) => (
  <Row type="flex" align="middle" className="HeaderContainer">
    <Col xs={10} sm={12} md={12} lg={12}>
      <a href="/">
        <img className="logo" src="atala-logo.svg" alt="Atala Logo" />
      </a>
    </Col>
    <Col
      xs={14}
      sm={{ span: 7, offset: 5 }}
      md={{ span: 7, offset: 5 }}
      lg={{ span: 4, offset: 8 }}
    >
      <Col xs={6} sm={6} md={9} lg={9}>
        <img
          className="IconUniversity"
          src="icon-free-university.svg"
          alt="Free University Tbilisi"
        />
      </Col>
      <UserAvatar lockWallet={lockWallet} />
    </Col>
  </Row>
);

Header.propTypes = {
  api: PropTypes.shape({
    lockWallet: PropTypes.func
  }).isRequired
};

export default withApi(Header);
