import React from 'react';
import { graphql } from 'gatsby';
import HeaderBlog from '../components/headerBlog/headerBlog';
import FooterBlog from '../components/footer/BlogFooter';
import Sidebar from '../components/sidebar/sidebar';
import AuthorPill from '../components/author/authorpill';
import SEO from '../components/seo/seo';

import './blog.scss';

const Authors = ({ data }) => {
  const {
    authors: { nodes: authors },
    postsPerYear: { group: postsPerYear },
    recentPosts: { nodes: recentPosts }
  } = data;

  return (
    <div className="BlogContainer fade">
      <SEO title="Authors" />
      <HeaderBlog backTo="/app" />
      <div className="container-middle-section">
        <div className="SectionContainerAuthor">
          <div>
            <div className="containerHeader">
              <h2 class="h2">Authors</h2>
            </div>
            <div className="containerAuthor">
              {authors.map(author => (
                <AuthorPill {...author} />
              ))}
            </div>
          </div>
        </div>
        <Sidebar recentPosts={recentPosts} postsPerYear={postsPerYear} />
      </div>
      <FooterBlog />
    </div>
  );
};

export default Authors;

export const pageQuery = graphql`
  query {
    site {
      siteMetadata {
        title
      }
    }
    authors: allAuthorsJson {
      nodes {
        photo
        name
        title
        position
        company
        email
        youtube
        twitter
        linkedIn
        github
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
