import React from 'react';
import PropTypes from 'prop-types';
import { Avatar, Col, Tooltip } from 'antd';
import SettingsMenu from '../SettingsMenu/SettingsMenu';
import { getInitials } from '../../../../helpers/genericHelpers';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const MAX_DISPLAY_LENGTH = 10;

const UserAvatar = ({ logo }) => {
  const { session, logout } = useSession();
  const organisationName = session?.organisationName;
  const orgNameIsTruncated = organisationName?.length > MAX_DISPLAY_LENGTH;

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
        <Tooltip title={orgNameIsTruncated ? organisationName : ''}>
          <p className="UserLabel">{organisationName}</p>
        </Tooltip>
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
