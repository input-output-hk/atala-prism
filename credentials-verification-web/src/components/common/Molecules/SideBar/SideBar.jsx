import React from 'react';
import { withRouter } from 'react-router-dom';
import PropTypes from 'prop-types';
import groupIcon from '../../../../images/groupsIcon.svg';
import recipientsIcon from '../../../../images/recipientsIcon.svg';
import certificateIcon from '../../../../images/certificateIcon.svg';

import './_style.scss';

const SideMenu = ({ location: { pathname } }) => {
  const options = [
    { route: '/recipients', icon: recipientsIcon, name: 'Recipients' },
    { route: '/groups', icon: groupIcon, name: 'Groups' },
    { route: '/newCredential', icon: certificateIcon, name: 'New Credential' }
  ];

  return (
    <div className="SideMenu">
      {options.map(({ route, icon, name }) => (
        <a className={pathname === route ? '' : ''} href={route} key={name}>
          <img style={{ height: '30px', width: '30px' }} src={icon} alt={`${name} Icon`} />
        </a>
      ))}
    </div>
  );
};

SideMenu.propTypes = {
  location: PropTypes.shape({ pathname: PropTypes.string }).isRequired
};

export default withRouter(SideMenu);
