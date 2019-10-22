import React from 'react';

import './_style.scss';
import UserAvatar from '../../Atoms/UserAvatar/UserAvatar';

const Header = () => (
  <div className="HeaderContainer">
    <a href="/">
      <img className="logo" src="atala-logo.svg" alt="Atala Logo" />
    </a>
    <div className="RightSide">
      <img
        className="IconUniversity"
        src="icon-free-university.svg"
        alt="Free University Tbilisi"
      />
      <UserAvatar />
    </div>
  </div>
);

export default Header;
