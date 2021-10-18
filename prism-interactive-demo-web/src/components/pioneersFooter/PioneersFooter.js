import React from 'react';
import { Link } from 'gatsby';

import './_style.scss';

const PioneersFooter = () => (
  <footer className="footer">
    <div className="footerLinks">
      <div>
        <a
          href="https://legal.atalaprism.io/privacy-policy.html "
          target="_blank"
          rel="noopener noreferrer"
        >
          Privacy
        </a>
        <a
          href="https://legal.atalaprism.io/terms-and-conditions.html "
          target="_blank"
          rel="noopener noreferrer"
        >
          Terms
        </a>
      </div>
      <Link to="/">AtalaPrism.io</Link>
    </div>
  </footer>
);

export default PioneersFooter;
