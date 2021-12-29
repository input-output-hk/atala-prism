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
            siteUrl
          }
        }
      }
    `
  );

  const metaDescription = description || site.siteMetadata.description;
  const pageTitle = title ? `${title} | ${site.siteMetadata.title}` : site.siteMetadata.title;
  const { siteUrl } = site.siteMetadata;
  const image = `${siteUrl}/images/atala-prism-logo.png`;

  return (
    <Helmet
      htmlAttributes={{
        lang
      }}
      title={pageTitle}
      meta={[
        { name: 'description', content: metaDescription },
        { property: 'og:title', content: pageTitle },
        { property: 'og:description', content: metaDescription },
        { property: 'og:image', content: image },
        { property: 'og:image:alt', content: 'Atala PRISM' },
        { property: 'og:type', content: 'website' },
        { property: 'og:site_name', content: 'Atala PRISM' },
        { name: 'twitter:card', content: 'summary' },
        { name: 'twitter:site', content: '@InputOutputHK' },
        { name: 'twitter:creator', content: '@InputOutputHK' },
        { name: 'twitter:title', content: pageTitle },
        { name: 'twitter:description', content: metaDescription },
        { name: 'twitter:image', content: image },
        { name: 'twitter:image:alt', content: 'Atala PRISM' },
        { name: 'twitter:url', content: siteUrl }
      ].concat(meta)}
    >
      <script charset="utf-8" type="text/javascript" src="//js.hsforms.net/forms/v2.js" />
    </Helmet>
  );
}

SEO.defaultProps = {
  lang: 'en',
  meta: [],
  description: '',
  title: null
};

SEO.propTypes = {
  description: PropTypes.string,
  lang: PropTypes.string,
  meta: PropTypes.arrayOf(PropTypes.object),
  title: PropTypes.string
};

export default SEO;
