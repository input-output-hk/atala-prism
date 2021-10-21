import React, { useEffect } from 'react';
import { graphql, Link } from 'gatsby';
import firebase from 'gatsby-plugin-firebase';
import logo from '../../static/images/logo-pioneer.svg';
import pioneers from '../images/pioneers.svg';
import triangle from '../../static/images/triangle.svg';
import triangleWhite from '../../static/images/triangleWhite.svg';
import Collapsable from '../components/collapsable/collapsable';
import CustomButton from '../components/customButton/CustomButton';
import PionnersFooter from '../components/pioneersFooter/PioneersFooter';
import SEO from '../components/seo/seo';
import { PIONEERS_EVENT } from '../helpers/constants';

import './pioneers.scss';

const Pioneers = ({ location }) => {
  const backTo = location && location.state && location.state.fromResources ? '/resources' : '/';

  useEffect(() => {
    firebase.analytics().logEvent(PIONEERS_EVENT);
  }, []);

  return (
    <div className="ContainerPioneers">
      <SEO title="Pioneers" />
      <div className="Pioneers">
        <div className="header">
          <img src={logo} alt="atala" />
          <a className="link" href={backTo}>
            Back
          </a>
        </div>
        <div className="FormContainer">
          <div className="Description">
            <div className="Container">
              <h3>Atala PRISM</h3>
              <h2>Pioneer Program</h2>
              <p className="subtitle">What is the Atala PRISM Pioneer Program?</p>
              <p className="bigger">
                It is a program to train developers to use the PRISM SDK to build solutions in the
                Cardano ecosystem.
              </p>
              <br />
              <p>
                Upon joining, you will become part of a select group with early access to a set of
                courses that will teach you the core principles of Atala PRISM, including
                Self-sovereign identity, decentralized identifiers (DIDs), verifiable credentials
                (VCs), and current digital identity standards.
              </p>
              <br />
              <p>
                The course materials are highly interactive, with weekly videos, exercises, and Q&A
                sessions, along with exclusive access to the course creators and key experts. You
                will also be able to join a dedicated community channel, created to help Pioneers
                connect to each other.
              </p>
            </div>
          </div>
          <div className="Form">
            <div id="registerInterest" className="WhiteBox">
              <div className="Container">
                <h2>Register your interest</h2>
                <p>The pioneers for the first cohort have been selected.</p>
                <p>You can still register your interest for future cohorts.</p>
                <a
                  href="https://input-output.typeform.com/to/dhnOj4gT"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <CustomButton
                    buttonProps={{ className: 'theme-primary' }}
                    buttonText="Register"
                  />
                </a>
              </div>
              <img src={pioneers} alt="illus" />
            </div>
          </div>
        </div>
        <div className="VideoContainer">
          <h3>About the Atala PRISM Pioneer Program</h3>
          <video controls>
            <source src="/videos/prism-pioneer-program-course-overview.mp4" type="video/mp4" />
          </video>
        </div>
      </div>
      <div className="center">
        <img src={triangle} />
      </div>
      <div className="FAQs">
        <h3>FAQs</h3>
        <Collapsable />
      </div>
      <div className="banner">
        <img className="whiteTriangle" src={triangleWhite} />
        <div className="logo">
          <img src={logo} alt="logo" />
        </div>
        <h2>Pioneers Program</h2>
        <Link to={`/pioneers#registerInterest`}>
          <CustomButton
            buttonProps={{
              className: 'theme-primary'
            }}
            buttonText="Register Interest"
          />
        </Link>
      </div>
      <PionnersFooter />
    </div>
  );
};

export default Pioneers;

export const pageQuery = graphql`
  query {
    site {
      siteMetadata {
        title
      }
    }
  }
`;
