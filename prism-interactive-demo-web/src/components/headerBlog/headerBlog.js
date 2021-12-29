import * as React from 'react';
import PropTypes from 'prop-types';
import Logo from '../../images/logo-atala.svg';
import BackArrow from './backArrow';

import './_style.scss';

const HeaderBlog = ({ backTo }) => (
  <header className="headerBlog">
    <div className="headerLogo">
      <img src={Logo} alt="atala-prism" />
    </div>
    <BackArrow backTo={backTo} />
  </header>
);

HeaderBlog.propTypes = {
  backTo: PropTypes.string.isRequired
};

export default HeaderBlog;
