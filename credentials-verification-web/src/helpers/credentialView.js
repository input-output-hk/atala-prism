import sanitizeHtml from 'sanitize-html';

const ISSUER_NAME_PLACEHOLDER = '{{issuer.name}}';

const HTML_SANITIZER_OPTIONS = {
  allowedTags: ['head', 'meta', 'body', 'div', 'p', 'h1', 'img', 'h3'],
  allowedSchemes: ['data'],
  allowedAttributes: {
    head: [],
    meta: ['name', 'content'],
    body: ['style'],
    div: ['style'],
    p: ['style'],
    h1: ['style'],
    img: ['style', 'src'],
    h3: ['style']
  }
};

export const fillHTMLCredential = (
  htmlTemplate,
  placeholders,
  credentialData,
  organisationName
) => {
  const credentialTemplate = Object.keys(placeholders)
    .reduce(
      (template, key) => template.replace(placeholders[key], credentialData[key]),
      htmlTemplate
    )
    .replace(ISSUER_NAME_PLACEHOLDER, organisationName)
    // The educational credential contains an unnecessary property
    // that breaks the styling during sanitization so here we remove it
    .replace('boxShadow;', '');

  return sanitizeView(credentialTemplate);
};

export const sanitizeView = html => sanitizeHtml(html, HTML_SANITIZER_OPTIONS);
