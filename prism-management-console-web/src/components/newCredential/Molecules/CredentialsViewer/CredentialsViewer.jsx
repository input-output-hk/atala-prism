import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import arrowLeft from '../../../../images/arrowLeft.svg';
import arrowRight from '../../../../images/arrowRight.svg';
import './_style.scss';

const DISPLAY_CONSTANTS = { startValue: 0, changeBy: 1 };

const CredentialsViewer = ({ credentialViews }) => {
  const { t } = useTranslation();

  const [displayedCredential, setDisplayedCredential] = useState(DISPLAY_CONSTANTS.startValue);
  const currentCredentialNumber = displayedCredential + DISPLAY_CONSTANTS.changeBy;
  const isLastCredential = currentCredentialNumber === credentialViews.length;
  const isFirstCredential = !displayedCredential;

  const onPrev = () => setDisplayedCredential(displayedCredential - DISPLAY_CONSTANTS.changeBy);
  const onNext = () => setDisplayedCredential(displayedCredential + DISPLAY_CONSTANTS.changeBy);

  return (
    <div className="PreviewContainer">
      {/* eslint-disable-next-line react/no-danger */}
      <div className="PreviewCredential">
        <div
          className="credentialScroll"
          dangerouslySetInnerHTML={{ __html: credentialViews[displayedCredential] }}
        />
      </div>
      <div className="arrowsContainer">
        <button type="button" onClick={onPrev} disabled={isFirstCredential}>
          <img src={arrowLeft} alt="left" />
        </button>
        {t('newCredential.credentialsPreview.displayedCredential', {
          current: currentCredentialNumber,
          outOf: credentialViews.length
        })}
        <button type="button" onClick={onNext} disabled={isLastCredential}>
          <img src={arrowRight} alt="right" />
        </button>
      </div>
    </div>
  );
};

CredentialsViewer.propTypes = {
  credentialViews: PropTypes.arrayOf(PropTypes.string).isRequired
};

export default CredentialsViewer;
