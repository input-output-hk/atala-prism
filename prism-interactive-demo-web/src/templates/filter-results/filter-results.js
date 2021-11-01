import React from 'react';
import PropTypes from 'prop-types';
import { Link, graphql } from 'gatsby';
import HeaderBlog from '../../components/headerBlog/headerBlog';
import FooterBlog from '../../components/footer/BlogFooter';
import calendarIcon from '../../images/calendar.svg';
import authorIcon from '../../images/author.svg';
import clockIcon from '../../images/clock.svg';
import SEO from '../../components/seo/seo';
import Sidebar from '../../components/sidebar/sidebar';
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
            {allPosts.map(post => {
              const title = post.frontmatter.title || post.fields.slug;

              return (
                <div className="mainSectionContainer">
                  <div className="articlesContainer">
                    <article
                      className="post-list-item"
                      itemScope
                      itemType="https://schema.org/Article"
                      key={post.fields.slug}
                    >
                      <header className="entryHeader">
                        <Link to={post.fields.slug} itemProp="url">
                          <h2 className="h2" itemProp="headline">
                            {title}
                          </h2>
                        </Link>
                      </header>
                      <div className="blog-post-container">
                        <section className="blog-post">
                          <p className="copete" itemProp="description">
                            {post.frontmatter.description}
                          </p>
                          <div className="postInfoContainer">
                            <div className="postInfo">
                              <div className="postInfoImgContainer">
                                <img src={calendarIcon} alt="date" />
                              </div>
                              <div>
                                <p>{post.frontmatter.date}</p>
                              </div>
                            </div>
                            <div className="postInfo">
                              <div className="postInfoImgContainer">
                                <img src={authorIcon} alt="author" />
                              </div>
                              <div>
                                <p>{post.frontmatter.author}</p>
                              </div>
                            </div>
                            <div className="postInfo">
                              <div className="postInfoImgContainer">
                                <img src={clockIcon} alt="readingTime" />
                              </div>
                              <div>
                                <p>{post.frontmatter.readingTime} mins read</p>
                              </div>
                            </div>
                          </div>
                          {post.frontmatter.image && (
                            <img
                              className="imgBlogPost"
                              src={post.frontmatter.image.publicURL}
                              alt="thumbnail"
                            />
                          )}
                          <section
                            className="post-article"
                            // eslint-disable-next-line react/no-danger
                            dangerouslySetInnerHTML={{ __html: post.html }}
                            itemProp="articleBody"
                          />
                          <Link to={post.fields.slug} itemProp="url">
                            <p className="viewMore">Read More</p>
                          </Link>
                        </section>
                        <section className="sideBar" />
                      </div>
                    </article>
                  </div>
                </div>
              );
            })}
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
