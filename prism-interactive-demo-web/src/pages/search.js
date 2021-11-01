import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Link, graphql } from 'gatsby';
import queryString from 'query-string';
import Highlighter from 'react-highlight-words';
import HeaderBlog from '../components/headerBlog/headerBlog';
import FooterBlog from '../components/footer/BlogFooter';
import calendarIcon from '../images/calendar.svg';
import authorIcon from '../images/author.svg';
import clockIcon from '../images/clock.svg';
import SEO from '../components/seo/seo';
import Sidebar from '../components/sidebar/sidebar';
import { getTextToHighlight } from '../helpers/textFormatter';
import { buildSearchIndex } from '../helpers/searchIndex';
import { groupedPostsShape, postsShape, recentPostsShape } from '../helpers/propTypes';

import './blog.scss';

const Search = ({ data, location }) => {
  const {
    posts: { nodes: posts },
    postsPerYear: { group: postsPerYear },
    recentPosts: { nodes: recentPosts }
  } = data;

  const search = location.search ? queryString.parse(location.search) : {};

  const [searchIndex, setSearchIndex] = useState();
  const [filteredPosts, setFilteredPosts] = useState([]);

  useEffect(() => {
    if (posts) {
      const dataToSearch = buildSearchIndex(posts);
      setSearchIndex(dataToSearch);
    }
  }, [posts]);

  useEffect(() => {
    if (search.query && searchIndex) {
      const queryResult = searchIndex.search(search.query);
      setFilteredPosts(queryResult);
    }
  }, [searchIndex, search.query]);

  return (
    <div className="BlogContainer fade">
      <SEO title="Blog" />
      <HeaderBlog backTo="/blog" />
      <div className="container-middle-section">
        <div className="SectionContainer">
          <div className="containerEntry">
            {!filteredPosts.length && <p>No results found. Try searching for full words.</p>}
            {filteredPosts.map(post => {
              const title = post.frontmatter.title || post.fields.slug;

              const displayBody = getTextToHighlight(post.html, search.query);

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
                            <Highlighter
                              highlightClassName="Highlighted"
                              searchWords={[search.query]}
                              textToHighlight={title}
                              autoEscape
                            />
                          </h2>
                        </Link>
                      </header>
                      <div className="blog-post-container">
                        <section className="blog-post">
                          <p className="copete" itemProp="description">
                            <Highlighter
                              highlightClassName="Highlighted"
                              searchWords={[search.query]}
                              textToHighlight={post.frontmatter.description}
                              autoEscape
                            />
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
                          <Highlighter
                            highlightClassName="Highlighted"
                            searchWords={[search.query]}
                            textToHighlight={displayBody}
                            autoEscape
                          />
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

Search.propTypes = {
  location: PropTypes.shape({
    search: PropTypes.string
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
    })
  }).isRequired
};

export default Search;

export const pageQuery = graphql`
  query SearchResults {
    site {
      siteMetadata {
        title
      }
    }
    posts: allMarkdownRemark(sort: { fields: [frontmatter___date], order: DESC }) {
      nodes {
        id
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
        internal {
          content
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
