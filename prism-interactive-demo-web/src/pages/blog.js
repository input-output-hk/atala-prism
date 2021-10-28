import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Link, graphql } from 'gatsby';
import firebase from 'gatsby-plugin-firebase';
import HeaderBlog from '../components/headerBlog/headerBlog';
import FooterBlog from '../components/footer/BlogFooter';
import calendarIcon from '../images/calendar.svg';
import authorIcon from '../images/author.svg';
import clockIcon from '../images/clock.svg';
import SEO from '../components/seo/seo';
import Sidebar from '../components/sidebar/sidebar';
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
      <HeaderBlog backTo="/app" />
      <div className="container-middle-section">
        <div className="SectionContainer">
          <div className="containerEntry">
            {allPosts.map(post => {
              const title = post.frontmatter.title || post.fields.slug;

              return (
                <div className="mainSectionContainer" key={post.fields.slug}>
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
                          <Link to={post.fields.slug} itemProp="url">
                            {post.frontmatter.image && (
                              <img
                                className="imgBlogPost"
                                src={post.frontmatter.image.publicURL}
                                alt="thumbnail"
                              />
                            )}
                          </Link>
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
