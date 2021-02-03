import React from 'react';
import PropTypes from 'prop-types';
import { DownOutlined } from '@ant-design/icons';
import { Menu, Dropdown, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../CustomButton/CustomButton';

import './_style.scss';

const menu = (handleLogout, logoutText) => (
  <Menu>
    <Menu.Item>
      <CustomButton
        buttonProps={{
          onClick: handleLogout,
          className: 'theme-primary'
        }}
        buttonText={logoutText}
      />
    </Menu.Item>
  </Menu>
);

const SettingsMenu = ({ logout }) => {
  const { t } = useTranslation();

  return (
    <Col className="SettingsMenu RightSide" xs={2} sm={2} md={2} lg={2}>
      <Dropdown
        className="logoutDropdown"
        overlay={menu(logout, t('menu.logout'))}
        trigger={['click']}
      >
        <div className="ant-dropdown-link">
          <DownOutlined />
        </div>
      </Dropdown>
    </Col>
  );
};

SettingsMenu.propTypes = {
  logout: PropTypes.func.isRequired
};

export default SettingsMenu;
