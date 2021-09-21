import * as React from 'react';
import { graphql } from 'gatsby';
import { Disqus } from 'gatsby-plugin-disqus';
import FooterBlog from '../../components/footer/footer';
import HeaderBlog from '../../components/headerBlog/headerBlog';
import calendarIcon from '../../images/calendar.svg';
import authorIcon from '../../images/author.svg';
import clockIcon from '../../images/clock.svg';
import SEO from '../../components/seo/seo';

import './_style.scss';
import '../../pages/blog.scss';

const BlogPostTemplate = ({ data, location, pageContext }) => {
  const post = data.markdownRemark;

  const backTo = location.state.fromResources ? '/resources' : '/blog';

  return (
    <div className="BlogContainer">
      <SEO title={post.frontmatter.title} description={post.excerpt} />
      <HeaderBlog backTo={backTo} />
      <div className="middleSectionContainer">
        <div className="mainSectionContainer">
          <article className="postSection" itemScope itemType="http://schema.org/Article">
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
                    <img src={calendarIcon} />
                  </div>
                  <div>
                    <small>{post.frontmatter.date}</small>
                  </div>
                </div>
                <div className="postInfo">
                  <div className="postInfoImgContainer">
                    <img src={authorIcon} />
                  </div>
                  <div>
                    <small>{post.frontmatter.author}</small>
                  </div>
                </div>
                <div className="postInfo">
                  <div className="postInfoImgContainer">
                    <img src={clockIcon} />
                  </div>
                  <div>
                    <small>{post.frontmatter.readingTime} mins read</small>
                  </div>
                </div>
              </div>
            </header>
            <div className="blog-post-container">
              {post.frontmatter.image && (
                <img className="imgBlogPost" src={post.frontmatter.image.publicURL} />
              )}
              <section
                className="post-article"
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

export default BlogPostTemplate;

export const pageQuery = graphql`
  query BlogPostBySlug($id: String!, $previousPostId: String, $nextPostId: String) {
    site {
      siteMetadata {
        title
      }
    }
    markdownRemark(id: { eq: $id }) {
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
