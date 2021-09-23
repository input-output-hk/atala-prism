import * as React from 'react';
import './_style.scss';

const PionnersFooter = () => {
  return (
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
        <a href="/">AtalaPrism.io</a>
      </div>
    </footer>
  );
};

export default PionnersFooter;
