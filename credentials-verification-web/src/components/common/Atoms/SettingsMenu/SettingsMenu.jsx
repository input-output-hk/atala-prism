import React from 'react';
import PropTypes from 'prop-types';
import { Menu, Dropdown, Icon, Col, message } from 'antd';
import { useHistory } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import CustomButton from '../CustomButton/CustomButton';

import './_style.scss';

const menu = (handleLogout, logoutText) => (
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
      <CustomButton
        buttonProps={{
          onClick: handleLogout,
          theme: ''
        }}
        buttonText={logoutText}
      />
    </Menu.Item>
  </Menu>
);

const SettingsMenu = ({ lockWallet }) => {
  const history = useHistory();
  const { t } = useTranslation();

  const logout = () =>
    lockWallet()
      .then(() => history.push('/'))
      .catch(() => message.error(t('errors.logout')));

  return (
    <Col className="SettingsMenu RightSide" xs={2} sm={2} md={2} lg={2}>
      <Dropdown overlay={menu(logout, t('menu.logout'))} trigger={['click']}>
        <div className="ant-dropdown-link">
          <Icon type="down" />
        </div>
      </Dropdown>
    </Col>
  );
};

SettingsMenu.propTypes = {
  lockWallet: PropTypes.func.isRequired
};

export default SettingsMenu;
