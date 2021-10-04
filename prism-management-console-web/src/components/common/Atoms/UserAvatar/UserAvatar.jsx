import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import SettingsMenu from '../SettingsMenu/SettingsMenu';
import { useSession } from '../../../../hooks/useSession';

import './_style.scss';

const UserAvatar = observer(({ logo }) => {
  const { session, logout } = useSession();
  const organisationName = session?.organisationName;

  return (
    <div className="UserAvatar">
      <SettingsMenu logout={logout} logo={logo} name={organisationName} />
    </div>
  );
});

UserAvatar.defaultProps = {
  logo: null
};

UserAvatar.propTypes = {
  logo: PropTypes.string
};

export default UserAvatar;
