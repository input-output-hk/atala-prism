import React from 'react';
import { Avatar } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';

import './_style.scss';

const UserAvatar = () => (
  <div className="UserAvatar">
    <div className="UserData">
      <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>JC</Avatar>
      <p className="UserLabel">John Case</p>
    </div>
    <SettingsMenu />
  </div>
);

export default UserAvatar;
