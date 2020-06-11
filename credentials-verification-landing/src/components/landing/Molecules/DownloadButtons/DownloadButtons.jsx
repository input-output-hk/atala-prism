import React from 'react';
import {
  APP_STORE_URL,
  GOOGLE_PLAY_STORE_URL,
  TESTFLIGHT_URL,
  ANDROID_APK_URL
} from '../../../../helpers/constants';

import './_style.scss';

const DownloadButtons = () => (
  <div className="DownloadButtons">
    <a href={TESTFLIGHT_URL} target="_blank" rel="noreferrer noopener">
      <img src="images/download-ios.png" alt="Download iOS" />
    </a>
    <a href={ANDROID_APK_URL} target="_blank" rel="noreferrer noopener">
      <img src="images/download-android.png" alt="Download Android" />
    </a>
  </div>
);

export default DownloadButtons;
