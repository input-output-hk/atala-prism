import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Menu } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import {
  VISION_NAME,
  DEMO_NAME,
  COMPONENTS_NAME,
  BENEFITS_NAME,
  USE_CASES_NAME,
  CASE_STUDY_NAME,
  GET_STARTED_NAME,
  FAQ_NAME,
  CONTACT_US_NAME
} from '../../../../helpers/constants';

import './_style.scss';

const NavBar = ({ executeScroll }) => {
  const { t } = useTranslation();

  const [current, setCurrent] = useState('mail');

  const handleClick = ({ key }) => {
    setCurrent(key);
    executeScroll(key);
  };

  const keys = [
    VISION_NAME,
    DEMO_NAME,
    COMPONENTS_NAME,
    BENEFITS_NAME,
    USE_CASES_NAME,
    CASE_STUDY_NAME,
    GET_STARTED_NAME,
    FAQ_NAME
  ];

  return (
    <Menu onClick={handleClick} selectedKeys={[current]} mode="horizontal" className="ulMain">
      {keys.map(key => (
        <Menu.Item key={key}>{t(`navBar.menuItems.${key}`)}</Menu.Item>
      ))}
      <Menu.Item key={CONTACT_US_NAME}>
        <CustomButton
          buttonProps={{
            className: 'theme-primary'
          }}
          buttonText={t(`navBar.menuItems.${CONTACT_US_NAME}`)}
        />
      </Menu.Item>
    </Menu>
  );
};

NavBar.propTypes = {
  executeScroll: PropTypes.func.isRequired
};

export default NavBar;
