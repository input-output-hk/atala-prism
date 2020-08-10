import React from 'react';
import { Avatar, Col } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';
import { getInitials } from '../../../../helpers/genericHelpers';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const UserAvatar = () => {
  const { session, logout } = useSession();
  const organisationName = session?.organisationName;

  return (
    <Col lg={6} className="UserAvatar">
      <div className="UserData">
        <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>
          {getInitials(organisationName)}
        </Avatar>
        <p className="UserLabel">{organisationName}</p>
      </div>
      <SettingsMenu logout={logout} />
    </Col>
  );
};

export default UserAvatar;
