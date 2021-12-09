import sanitizeHtml from 'sanitize-html';
import { dateFormat } from './formatters';

const ISSUER_NAME_PLACEHOLDER = '{{issuer.name}}';
const ISSUANCE_DATE_PLACEHOLDER = '{{issueDate}}';

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

const replacePlaceholdersFromObject = (html, placeholders, data) =>
  Object.keys(placeholders).reduce(
    (template, key) => template.replace(placeholders[key], data[key]),
    html
  );

export const autoReplacePlaceholders = (htmlTemplate, credentialData) =>
  htmlTemplate.replace(/{{(\w+)}}/g, (mustache, key) => credentialData[key] || mustache);

export const fillHTMLCredential = (
  htmlTemplate,
  credentialType,
  credentialData,
  organisationName
) => {
  const { isMultiRow, multiRowKey, multiRowView } = credentialType;

  /**
   * multi-row credentials are not supported by the backend
   * TODO: add support once the backend is ready. otherwise remove legacy code.
   */
  const multiRowHTML = isMultiRow
    ? credentialData[multiRowKey]
        .map(data =>
          replacePlaceholdersFromObject(multiRowView.html, multiRowView.placeholders, data)
        )
        .join('')
    : '';

  const credentialTemplateWithValues = autoReplacePlaceholders(htmlTemplate, credentialData);

  const processedCredentialTemplateWithValues = credentialTemplateWithValues
    .replace(ISSUER_NAME_PLACEHOLDER, organisationName)
    .replace(ISSUANCE_DATE_PLACEHOLDER, dateFormat(new Date()))
    .replace(`{{${multiRowKey}Html}}`, multiRowHTML)
    // The educational credential contains an unnecessary property
    // that breaks the styling during sanitization so here we remove it
    .replace('boxShadow;', '');

  return sanitizeView(processedCredentialTemplateWithValues);
};

export const sanitizeView = html => sanitizeHtml(html, HTML_SANITIZER_OPTIONS);
