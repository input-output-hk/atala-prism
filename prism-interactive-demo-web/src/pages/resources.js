import React from 'react';
import { graphql } from 'gatsby';
import Header from '../components/Header/Header';
import FooterBlog from '../components/footer/footer';
import SEO from '../components/seo/seo';
import FaqPanel from '../app/components/landing/Molecules/FaqPanel/FaqPanel';
import ContactPanel from '../app/components/landing/Organisms/ContactPanel/ContactPanel';
import { RESOURCES_NAME } from '../helpers/constants';

import './resources.scss';

const Resources = ({}) => {
  return (
    <div className="ResourcesContainer">
      <SEO title="Resources" />
      <Header currentSection={RESOURCES_NAME} />
      <div className="container-middle-section">
        <div className="videos-section">
          <h1>Video</h1>
          <div className="featured-video-container">
            <img className="thumbnail" src="/images/thumbnail.png" alt="Thumbnail" />
            <div className="video-info-container">
              <h2>Cardano Africa: Atala PRISM an explainer</h2>
              <p>
                Here's David with the lowdown on our DID (Decentralized ID) solution, deployed as
                part of our partnership with the Government of Ethiopia.
              </p>
              <div className="video-info">
                <a
                  className="watch-now"
                  href="https://www.youtube.com"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <img src="/images/play.svg" alt="play" />
                  <p>Watch now</p>
                </a>
                <div className="watch-time">
                  <img src="/images/watch-time.svg" alt="watch-time" />
                  <p>6:15 mins run time</p>
                </div>
              </div>
            </div>
          </div>
          <div className="videos-container">
            <div className="video-container">
              <img className="thumbnail" src="/images/thumbnail.png" alt="Thumbnail" />
              <div className="video-info-container">
                <h2>CardanoAfrica: Atala PRISM and digital identity</h2>
                <p>
                  Atala PRISM is a decentralized identity solution developed by Input Output for
                  Cardano.
                </p>
                <div className="video-info">
                  <a
                    className="watch-now"
                    href="https://www.youtube.com"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <img src="/images/play.svg" alt="play" />
                    <p>Watch now</p>
                  </a>
                  <div className="watch-time">
                    <img src="/images/watch-time.svg" alt="watch-time" />
                    <p>6:15 mins run time</p>
                  </div>
                </div>
              </div>
            </div>
            <div className="video-container">
              <img className="thumbnail" src="/images/thumbnail.png" alt="Thumbnail" />
              <div className="video-info-container">
                <h2>CardanoAfrica: our partnership with Ethiopia’s Ministry of Education</h2>
                <p>
                  Atala PRISM is a decentralized identity solution developed by Input Output for
                  Cardano.
                </p>
                <div className="video-info">
                  <a
                    className="watch-now"
                    href="https://www.youtube.com"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <img src="/images/play.svg" alt="play" />
                    <p>Watch now</p>
                  </a>
                  <div className="watch-time">
                    <img src="/images/watch-time.svg" alt="watch-time" />
                    <p>6:15 mins run time</p>
                  </div>
                </div>
              </div>
            </div>
            <div className="video-container">
              <img className="thumbnail" src="/images/thumbnail.png" alt="Thumbnail" />
              <div className="video-info-container">
                <h2>Cardano Africa - Atala DID technical walkthrough</h2>
                <p>
                  In this video, technical architect Alexis Hernandez shows Atala PRISM being used.
                </p>
                <div className="video-info">
                  <a
                    className="watch-now"
                    href="https://www.youtube.com"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <img src="/images/play.svg" alt="play" />
                    <p>Watch now</p>
                  </a>
                  <div className="watch-time">
                    <img src="/images/watch-time.svg" alt="watch-time" />
                    <p>6:15 mins run time</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        {/* <video controls>
          <source src="videos/Atix.webm" type="video/webm" />
        </video>
        <iframe
          src="https://www.youtube.com/embed/nrUsyw8cacA"
          title="Wellcome to our space!"
          allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
          frameBorder="0"
          webkitallowfullscreen="true"
          mozallowfullscreen="true"
          allowFullScreen
        /> */}
      </div>
      <FaqPanel />
      <ContactPanel />
      <FooterBlog />
    </div>
  );
};

export default Resources;

export const pageQuery = graphql`
  query {
    site {
      siteMetadata {
        title
      }
    }
  }
`;
