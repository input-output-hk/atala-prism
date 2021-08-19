import * as React from 'react';
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
    {(email || twitter || linkedIn) && (
      <div className="authorSocial">
        {email && (
          <a href={`mailto:${email}`}>
            <img src={emailIcon} alt="Email" />
          </a>
        )}
        {youtube && (
          <a href={youtube}>
            <img src={youtubeIcon} alt="Youtube" />
          </a>
        )}
        {twitter && (
          <a href={twitter}>
            <img src={twitterIcon} alt="Twitter" />
          </a>
        )}
        {linkedIn && (
          <a href={linkedIn}>
            <img src={linkedInIcon} alt="LinkedIn" />
          </a>
        )}
        {github && (
          <a href={github}>
            <img src={githubIcon} alt="Github" />
          </a>
        )}
      </div>
    )}
  </div>
);

export default AuthorPill;
