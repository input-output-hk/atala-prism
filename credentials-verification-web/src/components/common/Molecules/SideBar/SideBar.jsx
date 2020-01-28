import React from 'react';
import { withRouter, Link } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import PropTypes from 'prop-types';
import connectionsIcon from '../../../../images/connectionsIcon.svg';
import iconMenu from '../../../../images/icon-menu.svg';
import iconGroups from '../../../../images/icon-groups.svg';
import iconCredentials from '../../../../images/icon-credentials.svg';
// import iconTransactions from '../../../../images/icon-transactions.svg';
import certificateIcon from '../../../../images/certificateIcon.svg';
import credentialSummaryIcon from '../../../../images/credentialSummaryIcon.svg';
import settingsIcon from '../../../../images/settingsIcon.svg';
import supportIcon from '../../../../images/supportIcon.svg';
import './_style.scss';
import { ISSUER, VERIFIER, USER_ROLE } from '../../../../helpers/constants';

const { Sider } = Layout;

const SideMenu = ({ location: { pathname } }) => {
  const icons = [
    { icon: iconMenu, path: '/', restrictedTo: [ISSUER, VERIFIER] },
    { icon: connectionsIcon, path: '/connections', restrictedTo: [ISSUER, VERIFIER] },
    { icon: iconGroups, path: '/groups', restrictedTo: [ISSUER] },
    { icon: iconCredentials, path: '/credentials', restrictedTo: [ISSUER] },
    { icon: certificateIcon, path: '/newCredential', restrictedTo: [ISSUER] },
    { icon: credentialSummaryIcon, path: '/credentialSummary', restrictedTo: [ISSUER] }
    // The next pages are not yet developed
    // { icon: paymentIcon, path: '/payment', restrictedTo: [ISSUER, VERIFIER] }
    // { icon: iconTransactions, path: '/transactions' }
  ];

  const iconsByRole = icons.filter(({ restrictedTo }) =>
    restrictedTo.includes(localStorage.getItem(USER_ROLE))
  );

  const bottomIcons = [
    { icon: settingsIcon, path: '/settings' },
    { icon: supportIcon, path: '/support' }
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
        {iconsByRole.map(({ icon, path }) => (
          <Menu.Item key={path}>
            <Link to={path}>
              <img src={icon} alt="Menu Icon" />
            </Link>
          </Menu.Item>
        ))}
      </Menu>
      <Menu mode="inline" defaultSelectedKeys={[pathname]}>
        {bottomIcons.map(({ icon, path }) => (
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
