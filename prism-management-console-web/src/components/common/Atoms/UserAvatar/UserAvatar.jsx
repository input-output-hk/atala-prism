import React from 'react';
import PropTypes from 'prop-types';
import SettingsMenu from '../SettingsMenu/SettingsMenu';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const UserAvatar = ({ logo }) => {
  const { session, logout } = useSession();
  const organisationName = session?.organisationName;

  return (
    <div className="UserAvatar">
      <SettingsMenu logout={logout} logo={logo} name={organisationName} />
    </div>
  );
};

UserAvatar.defaultProps = {
  logo: null
};

UserAvatar.propTypes = {
  logo: PropTypes.string
};

export default UserAvatar;
