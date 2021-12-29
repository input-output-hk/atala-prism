import React from 'react';
import Logo from '../../images/logo-atala.svg';
import Footer from '../../images/footer.svg';

import './_style.scss';

const BlogFooter = () => (
  <footer>
    <div className="footerImage">
      <img src={Footer} alt="footer" />
      <hr />
    </div>
    <div className="footerLinks">
      <img src={Logo} alt="Atala-Logo" />
      <div>
        <a
          href="https://legal.atalaprism.io/terms-and-conditions.html"
          target="_blank"
          rel="noopener noreferrer"
        >
          Terms and Conditions
        </a>
        <a
          href="https://legal.atalaprism.io/privacy-policy.html"
          target="_blank"
          rel="noopener noreferrer"
        >
          Privacy Policy
        </a>
      </div>
      <p className="redText">Copyright © 2021 IOHK</p>
    </div>
  </footer>
);

export default BlogFooter;
