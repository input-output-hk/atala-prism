import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { graphql } from 'gatsby';
import firebase from 'gatsby-plugin-firebase';
import HeaderBlog from '../components/headerBlog/headerBlog';
import FooterBlog from '../components/footer/BlogFooter';
import SEO from '../components/seo/seo';
import Sidebar from '../components/sidebar/sidebar';
import BlogEntry from '../components/blogEntry/BlogEntry';
import { BLOG_EVENT } from '../helpers/constants';
import { groupedPostsShape, postsShape, recentPostsShape } from '../helpers/propTypes';

import './blog.scss';

const BlogIndex = ({ data }) => {
  const {
    posts: { nodes: allPosts },
    postsPerYear: { group: postsPerYear },
    recentPosts: { nodes: recentPosts }
  } = data;

  useEffect(() => {
    firebase.analytics().logEvent(BLOG_EVENT);
  }, []);

  return (
    <div className="BlogContainer fade">
      <SEO title="Blog" />
      <HeaderBlog backTo="/" />
      <div className="container-middle-section">
        <div className="SectionContainer">
          <div className="containerEntry">
            {allPosts.map(post => (
              <BlogEntry post={post} />
            ))}
          </div>
        </div>
        <Sidebar recentPosts={recentPosts} postsPerYear={postsPerYear} />
      </div>
      <FooterBlog />
    </div>
  );
};

BlogIndex.propTypes = {
  data: PropTypes.shape({
    posts: PropTypes.shape({
      nodes: postsShape
    }),
    recentPosts: PropTypes.shape({
      nodes: recentPostsShape
    }),
    postsPerYear: PropTypes.shape({
      group: groupedPostsShape
    })
  }).isRequired
};

export default BlogIndex;

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
    site {
      siteMetadata {
        title
      }
    }
    posts: allMarkdownRemark(sort: { fields: [frontmatter___date], order: DESC }) {
      nodes {
        excerpt
        html
        fields {
          slug
        }
        frontmatter {
          date(formatString: "MMMM DD, YYYY")
          title
          description
          author
          readingTime
          image {
            publicURL
          }
        }
      }
    }
    recentPosts: allMarkdownRemark(sort: { fields: [frontmatter___date], order: DESC }, limit: 3) {
      nodes {
        fields {
          slug
        }
        frontmatter {
          date(formatString: "MMMM DD, YYYY")
          title
          author
        }
      }
    }
    postsPerYear: allMarkdownRemark(sort: { fields: [frontmatter___date], order: DESC }) {
      group(field: fields___year) {
        totalCount
        fieldValue
      }
    }
  }
`;
