import React from 'react';
import PropTypes from 'prop-types';
import { Menu } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import { Link } from 'gatsby';
import CustomButton from '../customButton/CustomButton';
import {
  VISION_NAME,
  DEMO_NAME,
  COMPONENTS_NAME,
  BENEFITS_NAME,
  USE_CASES_NAME,
  CASE_STUDY_NAME,
  GET_STARTED_NAME,
  FAQ_NAME,
  CONTACT_US_NAME,
  BLOG_NAME,
  RESOURCES_NAME,
  PIONEERS_NAME
} from '../../helpers/constants';

import './_style.scss';

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

const NavBar = ({ currentSection }) => {
  const { t } = useTranslation();

  return (
    <Menu selectedKeys={[currentSection]} mode="horizontal" className="ulMain">
      {keys.map(key => (
        <Menu.Item key={key}>
          <Link to={`/#${key}`}>{t(`navBar.menuItems.${key}`)}</Link>
        </Menu.Item>
      ))}
      <Menu.Item key={RESOURCES_NAME}>
        <Link to="/resources">{t(`navBar.menuItems.${RESOURCES_NAME}`)}</Link>
      </Menu.Item>
      <Menu.Item key={PIONEERS_NAME}>
        <Link to="/pioneers" state={{ fromResources: currentSection === RESOURCES_NAME }}>
          {t(`navBar.menuItems.${PIONEERS_NAME}`)}
        </Link>
      </Menu.Item>
      <Menu.Item key={BLOG_NAME}>
        <Link to="/blog">{t(`navBar.menuItems.${BLOG_NAME}`)}</Link>
      </Menu.Item>
      <Menu.Item key={CONTACT_US_NAME}>
        <Link to={`/#${CONTACT_US_NAME}`}>
          <CustomButton
            buttonProps={{
              className: 'theme-primary'
            }}
            buttonText={t(`navBar.menuItems.${CONTACT_US_NAME}`)}
          />
        </Link>
      </Menu.Item>
    </Menu>
  );
};

NavBar.propTypes = {
  currentSection: PropTypes.oneOf(keys).isRequired
};

export default NavBar;
