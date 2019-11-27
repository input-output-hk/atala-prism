import React from 'react';
import { Row, Col } from 'antd';
import './_style.scss';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';

const Header = () => (
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
      <UserAvatar />
    </Col>
  </Row>
);

export default Header;
