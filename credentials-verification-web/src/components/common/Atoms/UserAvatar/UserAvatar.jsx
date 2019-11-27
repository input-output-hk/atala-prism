import React from 'react';
import { Avatar, Col } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';

import './_style.scss';

const UserAvatar = () => (
  <Col xs={18} sm={18} md={15} lg={15} className="UserAvatar">
    <Col xs={22} sm={22} md={22} lg={22} className="UserData">
      <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>JC</Avatar>
      <p className="UserLabel">John Case</p>
    </Col>
    <SettingsMenu />
  </Col>
);

export default UserAvatar;
