import * as React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'gatsby';
import author from '../../images/user.svg';
import emailIcon from '../../images/mail.svg';
import youtubeIcon from '../../images/youtube.svg';
import twitterIcon from '../../images/twitter.svg';
import linkedInIcon from '../../images/linkedin.svg';
import githubIcon from '../../images/github.svg';

import './_style.scss';

const AuthorPill = ({
  photo,
  name,
  title,
  position,
  company,
  email,
  youtube,
  twitter,
  linkedIn,
  github
}) => (
  <div className="pillContainer">
    <div className="authorInfo">
      <div>
        <img src={photo || author} alt="Author" />
      </div>
      <Link to={`/authors/${name.toLowerCase().replace(' ', '-')}`}>
        <h3>{name}</h3>
      </Link>
      <p>{title}</p>
    </div>
    <div className="positionInfo">
      <p>{position}</p>
      {company && <p>{company}</p>}
    </div>
    <div className="authorSocial">
      {[
        { href: email ? `mailto:${email}` : '', src: emailIcon, alt: 'Email' },
        { href: youtube || '', src: youtubeIcon, alt: 'Youtube' },
        { href: twitter || '', src: twitterIcon, alt: 'Twitter' },
        { href: linkedIn || '', src: linkedInIcon, alt: 'LinkedIn' },
        { href: github || '', src: githubIcon, alt: 'Github' }
      ].map(
        ({ href, src, alt }) =>
          href && (
            <a href={href} key={alt}>
              <img src={src} alt={alt} />
            </a>
          )
      )}
    </div>
  </div>
);

AuthorPill.defaultProps = {
  photo: null,
  company: null,
  email: null,
  youtube: null,
  twitter: null,
  linkedIn: null,
  github: null
};

AuthorPill.propTypes = {
  photo: PropTypes.string,
  name: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  position: PropTypes.string.isRequired,
  company: PropTypes.string,
  email: PropTypes.string,
  youtube: PropTypes.string,
  twitter: PropTypes.string,
  linkedIn: PropTypes.string,
  github: PropTypes.string
};

export default AuthorPill;
