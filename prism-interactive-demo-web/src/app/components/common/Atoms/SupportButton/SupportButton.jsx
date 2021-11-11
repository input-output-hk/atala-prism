import React from 'react';
import firebase from 'gatsby-plugin-firebase';
import CustomButton from '../../../../../components/customButton/CustomButton';
import { SUPPORT_EVENT } from '../../../../../helpers/constants';

import './_style.scss';

const helpIcon = {
  src: '/images/icon-help.svg',
  alt: 'HelpSupport'
};

const SupportButton = () => (
  <div className="SupportButton">
    <a
      href="https://iohk.zendesk.com/hc/en-us/requests/new"
      target="_blank"
      rel="noopener noreferrer"
    >
      <CustomButton
        buttonProps={{
          className: 'theme-primary',
          onClick: () => firebase.analytics().logEvent(SUPPORT_EVENT)
        }}
        img={helpIcon}
      />
    </a>
  </div>
);

export default SupportButton;
