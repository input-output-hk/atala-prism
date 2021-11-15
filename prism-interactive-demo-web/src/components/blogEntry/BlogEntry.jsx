import React from 'react';
import { Link } from 'gatsby';
import PropTypes from 'prop-types';
import Highlighter from 'react-highlight-words';
import { postShape } from '../../helpers/propTypes';
import calendarIcon from '../../images/calendar.svg';
import authorIcon from '../../images/author.svg';
import clockIcon from '../../images/clock.svg';
import { getTextToHighlight } from '../../helpers/textFormatter';

const BlogEntry = ({ post, query }) => {
  const title = post.frontmatter.title || post.fields.slug;
  const displayBody = query ? getTextToHighlight(post.html, query) : post.html;
  const { description } = post.frontmatter;

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
                {query ? (
                  <Highlighter
                    highlightClassName="Highlighted"
                    searchWords={[query]}
                    textToHighlight={title}
                    autoEscape
                  />
                ) : (
                  title
                )}
              </h2>
            </Link>
          </header>
          <div className="blog-post-container">
            <section className="blog-post">
              <p className="copete" itemProp="description">
                {query ? (
                  <Highlighter
                    highlightClassName="Highlighted"
                    searchWords={[query]}
                    textToHighlight={description}
                    autoEscape
                  />
                ) : (
                  description
                )}
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
              {query ? (
                <Highlighter
                  highlightClassName="Highlighted"
                  searchWords={[query]}
                  textToHighlight={displayBody}
                  autoEscape
                />
              ) : (
                <section
                  className="post-article"
                  // eslint-disable-next-line react/no-danger
                  dangerouslySetInnerHTML={{ __html: displayBody }}
                  itemProp="articleBody"
                />
              )}
              {!query && (
                <Link to={post.fields.slug} itemProp="url">
                  <p className="viewMore">Read More</p>
                </Link>
              )}
            </section>
            <section className="sideBar" />
          </div>
        </article>
      </div>
    </div>
  );
};

BlogEntry.defaultProps = {
  query: null
};

BlogEntry.propTypes = {
  post: postShape.isRequired,
  query: PropTypes.string
};

export default BlogEntry;
