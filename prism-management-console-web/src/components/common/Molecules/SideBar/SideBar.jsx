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
import templatesIcon from '../../../../images/templatesIcon.svg';

import './_style.scss';

const { Sider } = Layout;

const SUPPORT_URL = 'https://iohk.zendesk.com/hc/en-us/requests/new';

const SideMenu = ({ location: { pathname } }) => {
  const { t } = useTranslation();

  const icons = [
    { icon: iconMenu, name: '' },
    { icon: contactsIcon, name: 'contacts' },
    { icon: iconGroups, name: 'groups' },
    { icon: iconCredentials, name: 'credentials' },
    { icon: templatesIcon, name: 'templates' }
  ];

  return (
    <Sider width="65" breakpoint="md" collapsedWidth="0">
      <ReactTooltip place="right" />
      <Menu mode="inline" defaultSelectedKeys={[pathname]}>
        {icons.map(({ icon, name }) => {
          const path = `/${name}`;
          const itemName = t(`${name || 'dashboard'}.title`);

          return (
            <Menu.Item data-tip={itemName} key={path}>
              <Link to={path}>
                <img src={icon} alt={t('dashboard.itemAlt', { itemName })} />
              </Link>
            </Menu.Item>
          );
        })}
      </Menu>
      <Menu mode="inline" selectable={false}>
        <Menu.Item data-tip={t('support.title')}>
          <a href={SUPPORT_URL} rel="noopener noreferrer" target="_blank">
            <img
              src={supportIcon}
              alt={t('dashboard.itemAlt', { itemName: t('support.title') })}
              className="NoOpacity"
            />
          </a>
        </Menu.Item>
      </Menu>
    </Sider>
  );
};

SideMenu.propTypes = {
  location: PropTypes.shape({ pathname: PropTypes.string }).isRequired
};

export default withRouter(SideMenu);
