import React from 'react';
import PropTypes from 'prop-types';
import { graphql } from 'gatsby';
import HeaderBlog from '../../components/headerBlog/headerBlog';
import FooterBlog from '../../components/footer/BlogFooter';
import SEO from '../../components/seo/seo';
import Sidebar from '../../components/sidebar/sidebar';
import BlogEntry from '../../components/blogEntry/BlogEntry';
import { groupedPostsShape, postsShape, recentPostsShape } from '../../helpers/propTypes';

const FilterResults = ({ data, pageContext }) => {
  const { year } = pageContext;
  const {
    posts: { nodes: allPosts },
    postsPerYear: { group: postsPerYear },
    postsPerMonth: { group: postsPerMonth },
    recentPosts: { nodes: recentPosts }
  } = data;

  return (
    <div className="BlogContainer fade">
      <SEO title="Blog" />
      <HeaderBlog backTo="/blog" />
      <div className="container-middle-section">
        <div className="SectionContainer">
          <div className="containerEntry">
            {allPosts.map(post => (
              <BlogEntry post={post} />
            ))}
          </div>
        </div>
        <Sidebar
          selectedYear={year}
          recentPosts={recentPosts}
          postsPerYear={postsPerYear}
          postsPerMonth={postsPerMonth}
        />
      </div>
      <FooterBlog />
    </div>
  );
};

FilterResults.propTypes = {
  pageContext: PropTypes.shape({
    year: PropTypes.string
  }).isRequired,
  data: PropTypes.shape({
    posts: PropTypes.shape({
      nodes: postsShape
    }),
    recentPosts: PropTypes.shape({
      nodes: recentPostsShape
    }),
    postsPerYear: PropTypes.shape({
      group: groupedPostsShape
    }),
    postsPerMonth: PropTypes.shape({
      group: groupedPostsShape
    })
  }).isRequired
};

export default FilterResults;

export const pageQuery = graphql`
  query FilterResults($year: Int, $month: Int, $authorName: String) {
    site {
      siteMetadata {
        title
      }
    }
    posts: allMarkdownRemark(
      filter: {
        fields: { year: { eq: $year }, month: { eq: $month } }
        frontmatter: { author: { eq: $authorName } }
      }
      sort: { fields: [frontmatter___date], order: DESC }
    ) {
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
    postsPerMonth: allMarkdownRemark(
      filter: { fields: { year: { eq: $year } } }
      sort: { fields: [frontmatter___date], order: DESC }
    ) {
      group(field: fields___month) {
        totalCount
        fieldValue
      }
    }
  }
`;
