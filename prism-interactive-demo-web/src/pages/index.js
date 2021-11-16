import React from 'react';
import { graphql } from 'gatsby';
import SEO from '../components/seo/seo';
import Landing from '../components/landing/Landing';

const Main = () => (
  <div>
    <SEO />
    <Landing />
  </div>
);

export default Main;

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
