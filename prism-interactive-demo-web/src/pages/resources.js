import React from 'react';
import { graphql } from 'gatsby';
import Header from '../components/Header/Header';
import FooterBlog from '../components/footer/footer';
import SEO from '../components/seo/seo';
import { RESOURCES_NAME } from '../helpers/constants';

import './blog.scss';

const Resources = ({}) => {
  return (
    <div className="">
      <SEO title="Resources" />
      <Header currentSection={RESOURCES_NAME} />
      <div className="container-middle-section">
        <video controls>
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
        />
      </div>
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
