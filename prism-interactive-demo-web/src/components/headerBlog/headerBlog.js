import * as React from 'react';
import Logo from '../../images/logo-atala.svg';
import Back from '../../images/back.svg';
import './_style.scss';
import BackArrow from './backArrow';

const HeaderBlog = ({ backTo }) => {
  return (
    <header className="headerBlog">
      <div className="headerLogo">
        <img src={Logo} alt="atala-prism" />
      </div>
      <BackArrow backTo={backTo} />
    </header>
  );
};

export default HeaderBlog;
