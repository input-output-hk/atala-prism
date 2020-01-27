import React from 'react';
import PropTypes from 'prop-types';
import { Avatar, Col } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';

import './_style.scss';
import { ORGANISATION_NAME } from '../../../../helpers/constants';
import { getInitials } from '../../../../helpers/genericHelpers';

const UserAvatar = ({ lockWallet }) => {
  const organisationName = localStorage.getItem(ORGANISATION_NAME);

  return (
    <Col lg={6} className="UserAvatar">
      <div className="UserData">
        <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>
          {getInitials(organisationName)}
        </Avatar>
        <p className="UserLabel">{organisationName}</p>
      </div>
      <SettingsMenu lockWallet={lockWallet} />
    </Col>
  );
};

UserAvatar.propTypes = {
  lockWallet: PropTypes.func.isRequired
};

export default UserAvatar;
