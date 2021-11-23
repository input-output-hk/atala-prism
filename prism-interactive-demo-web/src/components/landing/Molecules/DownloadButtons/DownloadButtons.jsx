import React from 'react';
import { APP_STORE_URL, GOOGLE_PLAY_STORE_URL } from '../../../../helpers/constants';

import './_style.scss';

const DownloadButtons = () => (
  <div className="DownloadButtons">
    <a href={APP_STORE_URL} target="_blank" rel="noreferrer noopener">
      <img src="/images/download-ios-appStore.png" alt="Download iOS" />
    </a>
    <a href={GOOGLE_PLAY_STORE_URL} target="_blank" rel="noreferrer noopener">
      <img src="/images/download-android-googlePlay.png" alt="Download Android" />
    </a>
  </div>
);

export default DownloadButtons;
