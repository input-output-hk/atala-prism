import React from 'react';
import { Layout, Menu } from 'antd';
import './_style.scss';
import { withRouter, Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import iconMenu from '../../../../images/icon-menu.svg';
import iconRecipients from '../../../../images/icon-recipients.svg';
import iconGroups from '../../../../images/icon-groups.svg';
import iconCredentials from '../../../../images/icon-credentials.svg';
import iconTransactions from '../../../../images/icon-transactions.svg';

const { Sider } = Layout;

const SideMenu = ({ location: pathname }) => {
  const icons = [
    { icon: iconMenu, path: '' },
    { icon: iconRecipients, path: '/recipients' },
    { icon: iconGroups, path: '/groups' },
    { icon: iconCredentials, path: '/credentials' }
    // The next page is not yet developed
    // { icon: iconTransactions, path: '/transactions' }
  ];

  return (
    <Sider
      width="65"
      breakpoint="md"
      collapsedWidth="0"
      onBreakpoint={broken => {
        console.log(broken);
      }}
      onCollapse={(collapsed, type) => {
        console.log(collapsed, type);
      }}
    >
      <Menu mode="inline" defaultSelectedKeys={[pathname]}>
        {icons.map(({ icon, path }) => (
          <Menu.Item key={path}>
            <Link to={path}>
              <img src={icon} alt="Menu Icon" />
            </Link>
          </Menu.Item>
        ))}
      </Menu>
    </Sider>
  );
};

SideMenu.propTypes = {
  location: PropTypes.shape({ pathname: PropTypes.string }).isRequired
};

export default withRouter(SideMenu);
