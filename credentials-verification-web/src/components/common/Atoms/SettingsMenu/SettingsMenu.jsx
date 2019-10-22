import React from 'react';
import { Menu, Dropdown, Icon } from 'antd';

import './_style.scss';

const menu = (
  <Menu>
    <Menu.Item>
      <a target="_blank" rel="noopener noreferrer" href="http://www.alipay.com/">
        1st menu item
      </a>
    </Menu.Item>
    <Menu.Item>
      <a target="_blank" rel="noopener noreferrer" href="http://www.taobao.com/">
        2nd menu item
      </a>
    </Menu.Item>
    <Menu.Item>
      <a target="_blank" rel="noopener noreferrer" href="http://www.tmall.com/">
        3rd menu item
      </a>
    </Menu.Item>
  </Menu>
);

const SettingsMenu = () => (
  <div className="SettingsMenu">
    <Dropdown overlay={menu} trigger={['click']}>
      <div className="ant-dropdown-link">
        <Icon type="down" />
      </div>
    </Dropdown>
  </div>
);

export default SettingsMenu;
