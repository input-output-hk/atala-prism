import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { graphql } from 'gatsby';
import firebase from 'gatsby-plugin-firebase';
import { Disqus } from 'gatsby-plugin-disqus';
import FooterBlog from '../../components/footer/BlogFooter';
import HeaderBlog from '../../components/headerBlog/headerBlog';
import calendarIcon from '../../images/calendar.svg';
import authorIcon from '../../images/author.svg';
import clockIcon from '../../images/clock.svg';
import SEO from '../../components/seo/seo';
import { BLOG_POST_EVENT } from '../../helpers/constants';
import { postLinkShape, postShape } from '../../helpers/propTypes';

import './_style.scss';
import '../../pages/blog.scss';

const BlogPostTemplate = ({ data: { post }, location, pageContext }) => {
  const backTo =
    location && location.state && location.state.fromResources ? '/resources' : '/blog';

  useEffect(() => {
    firebase.analytics().logEvent(`${BLOG_POST_EVENT}${_.snakeCase(post.frontmatter.title)}`);
  }, []);

  return (
    <div className="BlogContainer">
      <SEO title={post.frontmatter.title} description={post.excerpt} />
      <HeaderBlog backTo={backTo} />
      <div className="middleSectionContainer">
        <div className="mainSectionContainer">
          <article className="postSection" itemScope itemType="https://schema.org/Article">
            <header>
              <h1 className="postHeader" itemProp="headline">
                {post.frontmatter.title}
              </h1>
              <p className="postDescription" itemProp="headline">
                {post.frontmatter.description}
              </p>
              <div className="postInfoContainer">
                <div className="postInfo">
                  <div className="postInfoImgContainer">
                    <img src={calendarIcon} alt="date" />
                  </div>
                  <div>
                    <small>{post.frontmatter.date}</small>
                  </div>
                </div>
                <div className="postInfo">
                  <div className="postInfoImgContainer">
                    <img src={authorIcon} alt="author" />
                  </div>
                  <div>
                    <small>{post.frontmatter.author}</small>
                  </div>
                </div>
                <div className="postInfo">
                  <div className="postInfoImgContainer">
                    <img src={clockIcon} alt="readingTime" />
                  </div>
                  <div>
                    <small>{post.frontmatter.readingTime} mins read</small>
                  </div>
                </div>
              </div>
            </header>
            <div className="blog-post-container">
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
            </div>
          </article>
        </div>
      </div>
      <Disqus
        config={{
          url: location.href,
          identifier: pageContext.id,
          title: post.frontmatter.title
        }}
      />
      <FooterBlog />
    </div>
  );
};

BlogPostTemplate.propTypes = {
  location: PropTypes.shape({
    href: PropTypes.string,
    state: PropTypes.shape({
      fromResources: PropTypes.bool
    })
  }).isRequired,
  pageContext: PropTypes.shape({
    id: PropTypes.string
  }).isRequired,
  data: PropTypes.shape({
    post: postShape,
    previous: postLinkShape,
    next: postLinkShape
  }).isRequired
};

export default BlogPostTemplate;

export const pageQuery = graphql`
  query BlogPostBySlug($id: String!, $previousPostId: String, $nextPostId: String) {
    site {
      siteMetadata {
        title
      }
    }
    post: markdownRemark(id: { eq: $id }) {
      id
      excerpt(pruneLength: 160)
      html
      frontmatter {
        title
        date(formatString: "MMMM DD, YYYY")
        description
        author
        readingTime
        image {
          publicURL
        }
      }
    }
    previous: markdownRemark(id: { eq: $previousPostId }) {
      fields {
        slug
      }
      frontmatter {
        title
      }
    }
    next: markdownRemark(id: { eq: $nextPostId }) {
      fields {
        slug
      }
      frontmatter {
        title
      }
    }
  }
`;
