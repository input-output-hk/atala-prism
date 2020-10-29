import React, { useState } from 'react';
import PropTypes from 'prop-types';
import sanitizeHtml from 'sanitize-html';
import { useTranslation } from 'react-i18next';
import { useSession } from '../../../providers/SessionContext';
import arrowLeft from '../../../../images/arrowLeft.svg';
import arrowRight from '../../../../images/arrowRight.svg';
import './_style.scss';

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

const DISPLAY_CONSTANTS = { startValue: 0, changeBy: 1 };

const CredentialsViewer = ({ credentialViewTemplate, credentialsData, credentialPlaceholders }) => {
  const { session } = useSession();
  const { t } = useTranslation();

  const [displayedCredential, setDisplayedCredential] = useState(DISPLAY_CONSTANTS.startValue);
  const currentCredentialNumber = displayedCredential + DISPLAY_CONSTANTS.changeBy;
  const isLastCredential = currentCredentialNumber === credentialsData.length;
  const isFirstCredential = !displayedCredential;

  const onPrev = () => setDisplayedCredential(displayedCredential - DISPLAY_CONSTANTS.changeBy);
  const onNext = () => setDisplayedCredential(displayedCredential + DISPLAY_CONSTANTS.changeBy);

  const credentialTemplate = Object.keys(credentialPlaceholders)
    .reduce(
      (htmlTemplate, key) =>
        htmlTemplate.replace(
          credentialPlaceholders[key],
          credentialsData[displayedCredential][key]
        ),
      credentialViewTemplate.htmltemplate
    )
    .replace(ISSUER_NAME_PLACEHOLDER, session.organisationName);

  const sanitizedHtml = sanitizeHtml(
    // The educational credential contains an unnecessary property
    // that breaks the styling during sanitization so here we remove it
    credentialTemplate.replace('boxShadow;', ''),
    HTML_SANITIZER_OPTIONS
  );

  return (
    <div className="PreviewContainer">
      {/* eslint-disable-next-line react/no-danger */}
      <div dangerouslySetInnerHTML={{ __html: sanitizedHtml }} />
      <div className="arrowsContainer">
        <button type="button" onClick={onPrev} disabled={isFirstCredential}>
          <img src={arrowLeft} alt="left" />
        </button>
        {t('newCredential.credentialsPreview.displayedCredential', {
          current: currentCredentialNumber,
          outOf: credentialsData.length
        })}
        <button type="button" onClick={onNext} disabled={isLastCredential}>
          <img src={arrowRight} alt="right" />
        </button>
      </div>
    </div>
  );
};

CredentialsViewer.propTypes = {
  credentialPlaceholders: PropTypes.objectOf(PropTypes.string).isRequired,
  credentialViewTemplate: PropTypes.shape({
    id: PropTypes.string,
    name: PropTypes.string,
    encodedlogoimage: PropTypes.string,
    logoimagemimetype: PropTypes.string,
    htmltemplate: PropTypes.string
  }).isRequired,
  credentialsData: PropTypes.arrayOf(PropTypes.objectOf(PropTypes.string)).isRequired
};

export default CredentialsViewer;
