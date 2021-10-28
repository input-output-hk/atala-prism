import { graphql } from 'gatsby';
import React from 'react';
import App from '../app/app';
import SEO from '../components/seo/seo';

const InteractiveDemoApp = () => {
  const isSsr = typeof window === 'undefined';
  return (
    <div>
      <SEO />
      {!isSsr && <App />}
    </div>
  );
};

export default InteractiveDemoApp;

// `src/app` is not "special", it is re-exported by `src/pages/app.js`
// and contains all the clientside dynamic App pages that we dont want to be statically generated.
// `src/pages/app.js` skips the static generation process because of
// `gatsby-plugin-create-client-paths` configured in `gatsby-config.js`

export const pageQuery = graphql`
  query {
    locales: allLocale(filter: { language: { eq: "en" } }) {
      edges {
        node {
          ns
          data
          language
        }
      }
    }
  }
`;
