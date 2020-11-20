import React from 'react';
import { withRouter, Link } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import PropTypes from 'prop-types';
import ReactTooltip from 'react-tooltip';
import { useTranslation } from 'react-i18next';
import contactsIcon from '../../../../images/connectionsIcon.svg';
import iconMenu from '../../../../images/icon-menu.svg';
import iconGroups from '../../../../images/icon-groups.svg';
import iconCredentials from '../../../../images/icon-credentials.svg';
import supportIcon from '../../../../images/supportIcon.svg';
import { useSession } from '../../../providers/SessionContext';
import { ISSUER, VERIFIER } from '../../../../helpers/constants';

import './_style.scss';

const { Sider } = Layout;

const SideMenu = ({ location: { pathname } }) => {
  const { t } = useTranslation();
  const { session } = useSession();

  const role = session.userRole;

  const icons = [
    { icon: iconMenu, name: '', restrictedTo: [ISSUER, VERIFIER] },
    { icon: contactsIcon, name: 'contacts', restrictedTo: [ISSUER, VERIFIER] },
    { icon: iconGroups, name: 'groups', restrictedTo: [ISSUER] },
    { icon: iconCredentials, name: 'credentials', restrictedTo: [ISSUER] }
  ];

  const iconsByRole = icons.filter(({ restrictedTo }) => restrictedTo.includes(role));

  const bottomIcons = [{ icon: supportIcon, name: 'support' }];

  // Tried to move it to a new component but it
  // lost the styles therefore making it hideous
  const getMenuItem = ({ icon, name }) => {
    const path = `/${name}`;
    const itemName = t(`${name || 'dashboard'}.title`);

    return (
      <Menu.Item data-tip={itemName} key={path}>
        <Link to={path}>
          <img src={icon} alt={t('dashboard.itemAlt', { itemName })} />
        </Link>
      </Menu.Item>
    );
  };

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
      <ReactTooltip place="right" />
      <Menu mode="inline" defaultSelectedKeys={[pathname]}>
        {iconsByRole.map(item => getMenuItem(item))}
      </Menu>
      <Menu mode="inline" defaultSelectedKeys={[pathname]}>
        {bottomIcons.map(item => getMenuItem(item))}
      </Menu>
    </Sider>
  );
};

SideMenu.propTypes = {
  location: PropTypes.shape({ pathname: PropTypes.string }).isRequired
};

export default withRouter(SideMenu);
