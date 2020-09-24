import React from 'react';
import PropTypes from 'prop-types';
import { Avatar, Col } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';
import { getInitials } from '../../../../helpers/genericHelpers';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const UserAvatar = ({ logo }) => {
  const { session, logout } = useSession();
  const organisationName = session?.organisationName;

  return (
    <Col lg={4} className="UserAvatar">
      <div className="UserData">
        {logo ? (
          <img className="IconUniversity" src={logo} alt="Free University Tbilisi" />
        ) : (
          <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>
            {getInitials(organisationName)}
          </Avatar>
        )}
        <p className="UserLabel">{organisationName}</p>
      </div>
      <SettingsMenu logout={logout} />
    </Col>
  );
};

UserAvatar.defaultProps = {
  logo: null
};

UserAvatar.propTypes = {
  logo: PropTypes.string
};

export default UserAvatar;
