import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Menu } from 'antd';
import { useTranslation } from 'react-i18next';
import { FEATURE_NAME, CREDENTIAL_NAME, DOWNLOAD_NAME } from '../../../../helpers/constants';

import './_style.scss';

const NavBar = ({ executeScroll }) => {
  const { t } = useTranslation();

  const [current, setCurrent] = useState('mail');

  const handleClick = ({ key }) => {
    setCurrent(key);
    executeScroll(key);
  };

  const keys = [FEATURE_NAME, CREDENTIAL_NAME, DOWNLOAD_NAME];

  return (
    <Menu onClick={handleClick} selectedKeys={[current]} mode="horizontal" className="ulMain">
      {keys.map(key => (
        <Menu.Item key={key}>{t(`navBar.menuItems.${key}`)}</Menu.Item>
      ))}
    </Menu>
  );
};

NavBar.propTypes = {
  executeScroll: PropTypes.func.isRequired
};

export default NavBar;
