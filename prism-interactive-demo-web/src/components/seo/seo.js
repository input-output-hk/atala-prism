import React from 'react';
import PropTypes from 'prop-types';
import { Helmet } from 'react-helmet';
import { useStaticQuery, graphql } from 'gatsby';

function SEO({ description, lang, meta, title }) {
  const { site } = useStaticQuery(
    graphql`
      query {
        site {
          siteMetadata {
            title
            description
            image
          }
        }
      }
    `
  );

  const metaDescription = description || site.siteMetadata.description;
  const pageTitle = title ? `${title} | ${site.siteMetadata.title}` : site.siteMetadata.title;
  const image = `https://www.atalaprism.io${site.siteMetadata.image}`;

  return (
    <Helmet
      htmlAttributes={{
        lang
      }}
      title={pageTitle}
      meta={[
        { name: `description`, content: metaDescription },
        { property: `og:title`, content: pageTitle },
        { property: `og:description`, content: metaDescription },
        { property: `og:image`, content: image },
        { property: `og:type`, content: `website` },
        { name: `twitter:card`, content: `summary` },
        { name: 'twitter:site', content: '@InputOutputHK' },
        { name: `twitter:title`, content: pageTitle },
        { name: `twitter:description`, content: metaDescription },
        { name: 'twitter:image', content: image }
      ].concat(meta)}
    />
  );
}

SEO.defaultProps = {
  lang: `en`,
  meta: [],
  description: ``
};

SEO.propTypes = {
  description: PropTypes.string,
  lang: PropTypes.string,
  meta: PropTypes.arrayOf(PropTypes.object),
  title: PropTypes.string.isRequired
};

export default SEO;
