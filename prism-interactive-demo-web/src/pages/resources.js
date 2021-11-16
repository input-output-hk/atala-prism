import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { graphql, Link } from 'gatsby';
import firebase from 'gatsby-plugin-firebase';
import _ from 'lodash';
import Header from '../components/Header/Header';
import FooterBlog from '../components/footer/BlogFooter';
import SEO from '../components/seo/seo';
import FaqPanel from '../components/landing/Molecules/FaqPanel/FaqPanel';
import ContactPanel from '../components/landing/Organisms/ContactPanel/ContactPanel';
import { RESOURCES_EVENT, RESOURCES_NAME } from '../helpers/constants';
import Play from '../images/play.svg';
import { brochureShape, postsShape, videoShape } from '../helpers/propTypes';

import './resources.scss';

const Resources = ({ data }) => {
  const {
    allVideosJson: { nodes: videos },
    allBrochuresJson: { nodes: brochures },
    allMarkdownRemark: {
      nodes: [latestPost]
    }
  } = data;

  useEffect(() => {
    firebase.analytics().logEvent(RESOURCES_EVENT);
  }, []);

  const featuredVideo = _.head(videos);
  const otherVideos = _.tail(videos);

  const featuredBrochure = _.head(brochures);
  const otherBrochures = _.tail(brochures);

  return (
    <div className="ResourcesContainer">
      <SEO title="Resources" />
      <Header currentSection={RESOURCES_NAME} />
      <div className="container-middle-section">
        <div className="videos-section">
          <h1 className="title">Videos</h1>
          <div className="featured-video-container">
            <div className="thumbnail">
              <img src={featuredVideo.thumbnail} alt="Thumbnail" />
            </div>
            <div className="video-info-container">
              <h2>{featuredVideo.title}</h2>
              <p>{featuredVideo.description}</p>
              <div className="video-info">
                <a
                  className="watch-now"
                  href={featuredVideo.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <img src={Play} alt="play" />
                  <p>Watch now</p>
                </a>
                <div className="watch-time">
                  <img src="/images/watch-time.svg" alt="watch-time" />
                  <p className="data">{featuredVideo.watchTime} mins run time</p>
                </div>
              </div>
            </div>
          </div>
          <div className="videos-container">
            {otherVideos.map(({ title, description, url, thumbnail, watchTime }) => (
              <div className="video-container">
                <img className="thumbnail" src={thumbnail} alt="Thumbnail" />
                <div className="video-info-container">
                  <h2>{title}</h2>
                  <p>{description}</p>
                  <div className="video-info">
                    <a className="watch-now" href={url} target="_blank" rel="noopener noreferrer">
                      <img src={Play} alt="play" />
                      <p>Watch now</p>
                    </a>
                    <div className="watch-time">
                      <img src="/images/watch-time.svg" alt="watch-time" />
                      <p className="data">{watchTime} mins run time</p>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="brochures-section">
          <h1 className="title">Brochures</h1>
          <div className="featured-brochure-container">
            <div className="brochure-info-container">
              <h2>{featuredBrochure.title}</h2>
              <p>{featuredBrochure.description}</p>
              <div className="brochure-info">
                <a
                  className="read"
                  href={featuredBrochure.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <img src="/images/pdf.svg" alt="pdf" />
                  <p>Read</p>
                </a>
              </div>
            </div>
            <img className="thumbnail" src={featuredBrochure.thumbnail} alt="Thumbnail" />
          </div>
          <div className="brochures-container">
            {otherBrochures.map(({ title, description, url, thumbnail }) => (
              <div className="brochure-container">
                <div className="brochure-info-container">
                  <img className="thumbnail" src={thumbnail} alt="Thumbnail" />
                  <h2>{title}</h2>
                  <p>{description}</p>
                </div>
                <div className="brochure-info">
                  <a className="read" href={url} target="_blank" rel="noopener noreferrer">
                    <img src="/images/pdf.svg" alt="pdf" />
                    <p>Read</p>
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      <div className="blog-section-container">
        <h1>Blog</h1>
        <div className="latest-post-container">
          <div className="blog-preview">
            <img src={latestPost.frontmatter.image.publicURL} alt="Blog preview" />
          </div>
          <div className="latest-post-info">
            <div>
              <h3>Latest post</h3>
              <h2>{latestPost.frontmatter.title}</h2>
              <p>{latestPost.frontmatter.description}</p>
            </div>
            <Link className="buttn" to={latestPost.fields.slug} state={{ fromResources: true }}>
              Blog
            </Link>
          </div>
        </div>
      </div>
      <FaqPanel />
      <ContactPanel />
      <FooterBlog />
    </div>
  );
};

Resources.propTypes = {
  data: PropTypes.shape({
    allVideosJson: PropTypes.shape({
      nodes: PropTypes.arrayOf(PropTypes.shape(videoShape))
    }),
    allBrochuresJson: PropTypes.shape({
      nodes: PropTypes.arrayOf(PropTypes.shape(brochureShape))
    }),
    allMarkdownRemark: postsShape
  }).isRequired
};

export default Resources;

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
    allBrochuresJson(sort: { fields: date, order: DESC }) {
      nodes {
        title
        url
        thumbnail
        description
      }
    }
    allVideosJson(sort: { fields: date, order: DESC }) {
      nodes {
        description
        thumbnail
        title
        url
        watchTime
      }
    }
    allMarkdownRemark(sort: { fields: [frontmatter___date], order: DESC }, limit: 1) {
      nodes {
        fields {
          slug
        }
        frontmatter {
          title
          description
          image {
            publicURL
          }
        }
      }
    }
  }
`;
