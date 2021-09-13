import React from 'react';
import { Link, graphql } from 'gatsby';
import logo from '../../static/images/logo-pioneer.svg';
import pioneers from '../images/pioneers.svg';
import FormPioneers from '../components/form/form';
import video from '../../static/images/video.png';
import triangle from '../../static/images/triangle.png';
import triangleWhite from '../../static/images/triangleWhite.png';
import Collapsable from '../components/collapsable/collapsable';
import CustomButton from '../components/customButton/CustomButton';
import PionnersFooter from '../components/pioneersFooter/footer';
import './pioneers.scss';

const BlogIndex = () => {
  return (
    <div className="ContainerPioneers">
      <div className="Pioneers">
        <div className="header">
          <img src={logo} alt="atala" />
          <a className="link" href="#">
            AtalaPRISM.io
          </a>
        </div>
        <div className="FormContainer">
          <div className="Description">
            <div className="Container">
              <h3>Atala PRISM</h3>
              <h2>
                Pioneers Program
              </h2>
              <p className="subtitle">What is the Atala PRISM Pioneer Program</p>
              <p>
                It is a program to recruit and train developers in the Atala PRISM environment,
                which is part of the greater Cardano environment.
              </p>
              <br />
              <p>
                Upon joining, you will become part of a select group with early access to a set of
                courses that will teach you the core principles of Atala PRISM, including
                Self-sovereign identity, decentralized identifiers (DIDs), and current digital
                identity standards.
              </p>
              <br />
              <p>
                The course materials are highly interactive, with weekly videos, exercises, and Q&A
                sessions, along with exclusive access to the creators and key experts.
              </p>
              <br />
              <p>
                You will also be able to join a dedicated community channel, created to help
                Pioneers connect to each other as you learn.
              </p>
            </div>
          </div>
          <div className="Form">
            <div className="WhiteBox">
              <div className="Container">
                <h2>Register interest</h2>
                <p>Please share your details and we will be in touch.</p>
                <FormPioneers />
              </div>
              <img src={pioneers} alt="illus" />
            </div>
          </div>
        </div>
        <div className="VideoContainer">
          <h3>About the Atala PRISM pioneer program</h3>
          <img src={video} alt="video" />
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
        <CustomButton buttonText="Register Interest" buttonProps={{ className: 'theme-primary' }} />
      </div>
      <PionnersFooter />
    </div>
  );
};

export default BlogIndex;

export const pageQuery = graphql`
  query {
    site {
      siteMetadata {
        title
      }
    }
  }
`;
