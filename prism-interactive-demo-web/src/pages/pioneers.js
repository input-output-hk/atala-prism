import React from 'react';
import { Link, graphql } from 'gatsby';
import HeaderBlog from '../components/headerBlog/headerBlog';
import FooterBlog from '../components/footer/footer';
import SEO from '../components/seo/seo';

import './blog.scss';

const BlogIndex = () => {
  return (
    <div className="BlogContainer fade">
      <SEO title="Pioneers" />
      <HeaderBlog backTo="/app" />
      <div className="container-middle-section" />
      <FooterBlog />
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
